// New F2 boundaries: selectors, defaults, lossless types, author-only content and server-owned quota.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const http = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}));
vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: http,
}));
import {
  realGenerateTestcases,
  realLoadDraft,
  realReadTestcaseContent,
  realRunReferenceSolution,
  specBody,
  toSpec,
} from "./real-authoring";

const P = "11111111-1111-4111-8111-111111111111";
const BASE = `/problem-bank/authoring/problems/${P}`;
const levels = {
  items: [{ id: "level-1", code: "EASY", displayName: "Easy", sortOrder: 1 }],
};
const generationInput = {
  problemId: P,
  body: "private statement",
  constraints: "private constraints",
  samples: [{ input: "private input", expected: "private expected" }],
};

describe("F2 authoring closure real boundaries", () => {
  beforeEach(() => Object.values(http).forEach((fn) => fn.mockReset()));
  afterEach(() => vi.unstubAllGlobals());

  it("includes a testcase selector only for the single-case request", async () => {
    http.post.mockResolvedValue({
      compileError: null,
      summary: { passed: 1, total: 1, allPass: true },
      results: [{ position: 7, verdict: "PASS", failure: null }],
    });
    await realRunReferenceSolution(
      P,
      { language: "Python", sourceCode: "print(1)" },
      "draft-testcase",
    );
    expect(http.post).toHaveBeenLastCalledWith(
      `${BASE}/reference-solution/run`,
      {
        language: "PYTHON",
        sourceCode: "print(1)",
        testcaseId: "draft-testcase",
      },
    );
    await realRunReferenceSolution(P, {
      language: "Python",
      sourceCode: "print(1)",
    });
    expect(http.post).toHaveBeenLastCalledWith(
      `${BASE}/reference-solution/run`,
      { language: "PYTHON", sourceCode: "print(1)" },
    );
  });

  it("round-trips an unknown server type and its fields without replacing it by OTHER", () => {
    const unknown = {
      kind: "MAP",
      keyType: { kind: "STRING" },
      valueType: { kind: "LONG" },
    };
    const wire = {
      matchingStrategy: "EXACT" as const,
      epsilonValue: null,
      stdinFormatMd: "in",
      stdoutFormatMd: "out",
      functionName: "solve",
      returnType: unknown,
      parameters: [{ name: "input", type: unknown }],
      nameOverrides: null,
    };
    const draft = toSpec(wire);
    expect(draft.signature.returnType.kind).toBe("OTHER");
    const saved = specBody(draft, "2026-11-10T00:00:00Z");
    expect(saved.returnType).toEqual(unknown);
    expect(saved.parameters?.[0]?.type).toEqual(unknown);
    expect(saved.functionName).toBe("solve");
  });

  it("loads new-form limits and generation cap from author defaults rather than hard-coded values", async () => {
    http.get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/levels") return levels;
      if (path === "/problem-bank/authoring/defaults")
        return {
          timeLimitMs: 3456,
          memoryLimitMb: 512,
          maxOutputSizeKb: 128,
          maxAiGenerationsPerProblem: 7,
        };
      throw new Error(`Unexpected GET ${path}`);
    });
    const record = await realLoadDraft();
    expect(record.draft.limits).toEqual({
      timeLimitMs: 3456,
      memoryLimitMb: 512,
      outputLimitKb: 128,
      maxSubmissionsPerHour: null,
    });
    expect(record.aiGeneration).toEqual({ used: 0, limit: 7 });
    expect(http.get).toHaveBeenCalledWith("/problem-bank/authoring/defaults");
  });

  it("does not silently substitute local limits when the defaults endpoint fails", async () => {
    const unavailable = new ApiError("PRB-050", 503);
    http.get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/levels") return levels;
      throw unavailable;
    });
    await expect(realLoadDraft()).rejects.toBe(unavailable);
  });

  it("fetches fresh author-only MinIO URLs and preserves the content bytes without application credentials", async () => {
    const contents = "  leading\ntrailing  \n";
    http.get.mockResolvedValue({
      testcases: [
        {
          id: "case-1",
          position: 1,
          visibility: "HIDDEN",
          origin: "MANUAL",
          approved: false,
          category: null,
          input: null,
          inputStorage: "MINIO",
          inputDownloadUrl: "https://storage.test/fresh-input",
          expectedOutput: "1\n",
          expectedOutputStorage: "INLINE",
        },
      ],
    });
    const fetch = vi
      .fn()
      .mockResolvedValue({
        ok: true,
        arrayBuffer: async () => new TextEncoder().encode(contents).buffer,
      });
    vi.stubGlobal("fetch", fetch);
    const row = await realReadTestcaseContent(P, "case-1");
    expect(http.get).toHaveBeenCalledWith(BASE);
    expect(fetch).toHaveBeenCalledWith(
      "https://storage.test/fresh-input",
      expect.objectContaining({ credentials: "omit" }),
    );
    expect(row).toMatchObject({
      input: contents,
      expected: "1\n",
      approved: false,
      visibility: "hidden",
    });
    expect(row.largeSides).toBeUndefined();
  });

  it("sends an empty generation body and trusts only the returned usage and lock", async () => {
    http.post.mockResolvedValue({
      testcases: [],
      dropped: [{ reason: "DUPLICATE_INPUT", count: 2 }],
      warnings: [],
      aiGeneration: { used: 4, limit: 7 },
      problemUpdatedAt: "2026-11-10T05:00:00Z",
    });
    const result = await realGenerateTestcases(generationInput);
    expect(http.post).toHaveBeenCalledExactlyOnceWith(
      `${BASE}/testcases/generate`,
      {},
    );
    expect(result).toMatchObject({
      aiGeneration: { used: 4, limit: 7 },
      problemUpdatedAt: "2026-11-10T05:00:00Z",
      dropped: [{ reason: "duplicateInput", count: 2 }],
    });
  });

  it("rejects generation before save and propagates a server quota failure unchanged", async () => {
    await expect(
      realGenerateTestcases({ ...generationInput, problemId: undefined }),
    ).rejects.toMatchObject({ code: "PRB-041", status: 422 });
    expect(http.post).not.toHaveBeenCalled();
    const exhausted = new ApiError("PRB-042", 422);
    http.post.mockRejectedValue(exhausted);
    await expect(realGenerateTestcases(generationInput)).rejects.toBe(
      exhausted,
    );
  });
});
