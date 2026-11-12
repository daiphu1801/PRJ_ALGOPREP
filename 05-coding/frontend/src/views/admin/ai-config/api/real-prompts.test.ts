import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const put = vi.fn();
const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
    post: (...args: unknown[]) => post(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realGetAiConfigPage,
  realPublishPrompts,
  realSavePromptDraft,
  toPage,
} from "./real-prompts";
import { withServerDrafts } from "../model/changes";

const BASE = "/ai/admin/prompt-templates";
const guards = { citeLines: true, answerInVietnamese: true, noFullSolution: true };

const active = (feature: string, extra: Record<string, unknown> = {}) => ({
  id: `active-${feature}`,
  featureCode: feature,
  version: "v3.8",
  status: "ACTIVE",
  systemInstruction: `live ${feature}`,
  modelId: "claude-sonnet",
  responseGuards: feature === "GENERATE_TESTCASE" ? null : guards,
  responseSchemaVersion: "1",
  maxTokens: 1600,
  publishedAt: "2026-08-11T05:00:00Z",
  createdAt: "2026-08-10T00:00:00Z",
  ...extra,
});

const listDto = {
  items: [
    active("SOLUTION_REVIEW"),
    active("MOCK_INTERVIEW"),
    {
      ...active("MOCK_INTERVIEW"),
      id: "draft-interview",
      status: "DRAFT",
      version: "v3.9",
      systemInstruction: "draft interview",
      modelId: "claude-haiku",
      responseGuards: { ...guards, citeLines: false },
      publishedAt: null,
    },
    active("GENERATE_TESTCASE", { publishedAt: null }),
  ],
  modelOptions: ["claude-sonnet", "claude-haiku"],
};

const historyDto = (feature: string) => ({
  items: [
    {
      id: `${feature}-draft`,
      version: "v9.9",
      status: "DRAFT",
      publishedAt: null,
      createdAt: "2026-08-12T00:00:00Z",
      systemInstruction: "hidden draft",
    },
    {
      id: `${feature}-active`,
      version: "v3.8",
      status: "ACTIVE",
      publishedAt: "2026-08-11T05:00:00Z",
      createdAt: "2026-08-10T00:00:00Z",
      systemInstruction: `live ${feature}`,
    },
    {
      id: `${feature}-old`,
      version: "v3.7",
      status: "ARCHIVED",
      publishedAt: null,
      createdAt: "2026-08-02T00:00:00Z",
      systemInstruction: "old",
    },
  ],
});

function serve() {
  get.mockImplementation(async (path: string) => {
    if (path === BASE) return listDto;
    const match = /^\/ai\/admin\/prompt-templates\/(\w+)\/history$/.exec(path);
    if (match) return historyDto(match[1]!);
    throw new Error(`unexpected GET ${path}`);
  });
}

const apiError = (code: string, status: number) =>
  new ApiError(code, status, "server text never shown");

beforeEach(() => {
  get.mockReset();
  put.mockReset();
  post.mockReset();
});

describe("realGetAiConfigPage (#14 + #15)", () => {
  it("reads the list and the history of every feature, nothing else", async () => {
    serve();
    await realGetAiConfigPage();
    expect(get.mock.calls.map((call) => call[0]).sort()).toEqual(
      [
        BASE,
        `${BASE}/SOLUTION_REVIEW/history`,
        `${BASE}/MOCK_INTERVIEW/history`,
        `${BASE}/GENERATE_TESTCASE/history`,
      ].sort(),
    );
  });

  it("maps ACTIVE rows to the screen model and keeps the stored DRAFT apart", async () => {
    serve();
    const page = await realGetAiConfigPage();
    expect(page.modelOptions).toEqual(["claude-sonnet", "claude-haiku"]);
    expect(page.activeIds).toEqual({
      review: "active-SOLUTION_REVIEW",
      interview: "active-MOCK_INTERVIEW",
      testcase: "active-GENERATE_TESTCASE",
    });
    expect(page.prompts[0]).toEqual({
      feature: "review",
      version: "v3.8",
      status: "live",
      modelId: "claude-sonnet",
      systemInstruction: "live SOLUTION_REVIEW",
      maxTokens: "1.600 token",
      updatedAt: "11/08/2026",
    });
    // publishedAt null falls back to createdAt.
    expect(page.prompts[2]!.updatedAt).toBe("10/08/2026");
    expect(page.guards).toEqual({ review: guards, interview: guards });
    expect(page.drafts).toEqual({
      interview: {
        systemInstruction: "draft interview",
        modelId: "claude-haiku",
        guards: { ...guards, citeLines: false },
      },
    });
    // Fields the screen does not read must not leak into the model.
    expect(JSON.stringify(page)).not.toContain("responseSchemaVersion");
  });

  it("builds the history without DRAFT rows and without an author", async () => {
    serve();
    const page = await realGetAiConfigPage();
    const review = page.versionHistory.filter((e) => e.feature === "review");
    expect(review).toEqual([
      {
        id: "SOLUTION_REVIEW-active",
        feature: "review",
        version: "v3.8",
        publishedAt: "11/08/2026",
        author: null,
        systemInstruction: "live SOLUTION_REVIEW",
      },
      {
        id: "SOLUTION_REVIEW-old",
        feature: "review",
        version: "v3.7",
        publishedAt: "02/08/2026",
        author: null,
        systemInstruction: "old",
      },
    ]);
    expect(page.versionHistory).toHaveLength(6);
  });

  it("the stored DRAFT becomes the editable copy, the live page stays untouched", async () => {
    serve();
    const page = await realGetAiConfigPage();
    const editable = withServerDrafts(page);
    expect(editable.prompts[1]).toMatchObject({
      systemInstruction: "draft interview",
      modelId: "claude-haiku",
      version: "v3.8",
      status: "live",
    });
    expect(editable.guards.interview.citeLines).toBe(false);
    expect(page.prompts[1]!.systemInstruction).toBe("live MOCK_INTERVIEW");
    expect(page.guards.interview.citeLines).toBe(true);
  });

  it("fails loudly when an ACTIVE row or the guards are missing (no invented values)", async () => {
    get.mockImplementation(async (path: string) =>
      path === BASE
        ? { ...listDto, items: [active("SOLUTION_REVIEW")] }
        : historyDto("X"),
    );
    await expect(realGetAiConfigPage()).rejects.toMatchObject({
      code: "UNKNOWN_ERROR",
    });
    get.mockImplementation(async (path: string) =>
      path === BASE
        ? {
            ...listDto,
            items: [
              active("SOLUTION_REVIEW", { responseGuards: null }),
              active("MOCK_INTERVIEW"),
              active("GENERATE_TESTCASE"),
            ],
          }
        : historyDto("X"),
    );
    await expect(realGetAiConfigPage()).rejects.toMatchObject({
      code: "UNKNOWN_ERROR",
    });
  });

  it.each([
    ["AIR-301", 403],
    ["AIR-V11", 400],
  ])("rethrows %s from the server", async (code, status) => {
    get.mockRejectedValue(apiError(code, status));
    await expect(realGetAiConfigPage()).rejects.toMatchObject({ code, status });
  });
});

describe("realSavePromptDraft (#16)", () => {
  it("PUTs the exact draft body of a guarded feature", async () => {
    put.mockResolvedValue({});
    await realSavePromptDraft({
      feature: "review",
      systemInstruction: "Chi thi moi",
      modelId: "claude-haiku",
      guards: { noFullSolution: false, citeLines: true, answerInVietnamese: false },
    });
    expect(put).toHaveBeenCalledWith(`${BASE}/SOLUTION_REVIEW/draft`, {
      systemInstruction: "Chi thi moi",
      modelId: "claude-haiku",
      responseGuards: {
        citeLines: true,
        answerInVietnamese: false,
        noFullSolution: false,
      },
    });
  });

  it("sends null guards for testcase generation", async () => {
    put.mockResolvedValue({});
    await realSavePromptDraft({
      feature: "testcase",
      systemInstruction: "x",
      modelId: "claude-haiku",
      guards: null,
    });
    expect(put).toHaveBeenCalledWith(`${BASE}/GENERATE_TESTCASE/draft`, {
      systemInstruction: "x",
      modelId: "claude-haiku",
      responseGuards: null,
    });
  });

  it.each([
    ["AIR-V11", 400],
    ["AIR-V12", 400],
    ["AIR-V13", 400],
    ["AIR-202", 422],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(apiError(code, status));
    await expect(
      realSavePromptDraft({
        feature: "interview",
        systemInstruction: "x",
        modelId: "m",
        guards,
      }),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("realPublishPrompts (#17)", () => {
  function pages() {
    const saved = toPage(listDto, []);
    const draft = toPage(listDto, [
      {
        id: "keep",
        feature: "review",
        version: "v3.8",
        publishedAt: "11/08/2026",
        author: null,
        systemInstruction: "kept",
      },
    ]);
    draft.prompts = draft.prompts.map((p) =>
      p.feature === "review"
        ? { ...p, systemInstruction: "edited", modelId: "claude-haiku" }
        : p,
    );
    draft.guards = {
      ...draft.guards,
      interview: { ...draft.guards.interview, noFullSolution: false },
    };
    draft.prompts = draft.prompts.map((p) =>
      p.feature === "testcase" ? { ...p, modelId: "claude-haiku" } : p,
    );
    return { saved, draft };
  }

  it("POSTs one atomic body: full values, the loaded active id, null guards for testcase", async () => {
    serve();
    post.mockResolvedValue(listDto);
    const { saved, draft } = pages();
    await realPublishPrompts({ saved, draft });
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith(`${BASE}/publish`, {
      changes: [
        {
          featureCode: "SOLUTION_REVIEW",
          expectedActivePromptId: "active-SOLUTION_REVIEW",
          systemInstruction: "edited",
          modelId: "claude-haiku",
          responseGuards: guards,
        },
        {
          featureCode: "MOCK_INTERVIEW",
          expectedActivePromptId: "active-MOCK_INTERVIEW",
          systemInstruction: "live MOCK_INTERVIEW",
          modelId: "claude-sonnet",
          responseGuards: { ...guards, noFullSolution: false },
        },
        {
          featureCode: "GENERATE_TESTCASE",
          expectedActivePromptId: "active-GENERATE_TESTCASE",
          systemInstruction: "live GENERATE_TESTCASE",
          modelId: "claude-haiku",
          responseGuards: null,
        },
      ],
    });
  });

  it("leaves an unchanged feature out of the body", async () => {
    serve();
    post.mockResolvedValue(listDto);
    const saved = toPage(listDto, []);
    const draft = toPage(listDto, []);
    draft.prompts = draft.prompts.map((p) =>
      p.feature === "review" ? { ...p, modelId: "claude-haiku" } : p,
    );
    await realPublishPrompts({ saved, draft });
    const body = post.mock.calls[0]![1] as { changes: { featureCode: string }[] };
    expect(body.changes.map((c) => c.featureCode)).toEqual(["SOLUTION_REVIEW"]);
  });

  it("returns the new live page of every feature with a fresh history", async () => {
    serve();
    post.mockResolvedValue({
      ...listDto,
      items: listDto.items.map((i) =>
        i.id === "active-SOLUTION_REVIEW"
          ? { ...i, id: "new-active", version: "v3.9" }
          : i,
      ),
    });
    const { saved, draft } = pages();
    const next = await realPublishPrompts({ saved, draft });
    expect(next.activeIds.review).toBe("new-active");
    expect(next.prompts[0]!.version).toBe("v3.9");
    expect(next.versionHistory).toHaveLength(6);
  });

  it("keeps the old log when only the history reload fails after a good publish", async () => {
    get.mockRejectedValue(apiError("AIR-V11", 400));
    post.mockResolvedValue(listDto);
    const { saved, draft } = pages();
    const next = await realPublishPrompts({ saved, draft });
    expect(next.versionHistory.map((e) => e.id)).toEqual(["keep"]);
  });

  it.each([
    ["AIR-V11", 400],
    ["AIR-V12", 400],
    ["AIR-V13", 400],
    ["AIR-V14", 400],
    ["AIR-201", 409],
    ["AIR-202", 422],
    ["AIR-205", 422],
    ["AIR-301", 403],
    ["AIR-503", 503],
  ])("rethrows %s and reads no history", async (code, status) => {
    post.mockRejectedValue(apiError(code, status));
    const { saved, draft } = pages();
    await expect(realPublishPrompts({ saved, draft })).rejects.toMatchObject({
      code,
      status,
    });
    expect(get).not.toHaveBeenCalled();
  });
});
