// Real authoring calls (03-dd/api/F2_problem-bank.md #33 to #47), only the HTTP layer is faked. The
// points that matter: the wire names and exact bodies, a save that stops half way reports what went
// through, the lock value is chained, and nothing is invented for a field the server does not send.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const put = vi.fn();
const patch = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    put: (...args: unknown[]) => put(...args),
    patch: (...args: unknown[]) => patch(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

import {
  emptySpec,
  realBulkUpload,
  realCreateTestcase,
  realDeleteTestcase,
  realLoadDraft,
  realReorderTestcases,
  realRunReferenceSolution,
  realSaveDraft,
  realUpdateTestcase,
  specBody,
  toSpec,
} from "./real-authoring";
import type { ProblemDraft, ServerState } from "../model/draft-types";

const A = "/problem-bank/authoring/problems";
const P = "11111111-1111-4111-8111-111111111111";
const LEVEL_EASY = "22222222-2222-4222-8222-222222222222";
const LEVEL_HARD = "33333333-3333-4333-8333-333333333333";
const TOPIC_1 = "44444444-4444-4444-8444-444444444444";
const TOPIC_2 = "55555555-5555-4555-8555-555555555555";
const EX_ID = "66666666-6666-4666-8666-666666666666";
const T0 = "2026-11-06T01:00:00Z";
const T1 = "2026-11-06T02:00:00Z";
const T2 = "2026-11-06T03:00:00Z";
const T3 = "2026-11-06T04:00:00Z";

const levelsDto = {
  items: [
    {
      id: LEVEL_EASY,
      code: "EASY",
      displayName: "De",
      sortOrder: 1,
      problemCount: null,
    },
    {
      id: LEVEL_HARD,
      code: "HARD",
      displayName: "Kho",
      sortOrder: 2,
      problemCount: null,
    },
  ],
};

const testcaseDto = (over: Record<string, unknown> = {}) => ({
  id: "tc-1",
  position: 1,
  visibility: "SAMPLE",
  origin: "MANUAL",
  approved: true,
  category: "BOUNDARY_LOW",
  input: "1 2",
  inputStorage: "INLINE",
  inputSizeBytes: 3,
  inputDownloadUrl: null,
  expectedOutput: "3",
  expectedOutputStorage: "INLINE",
  expectedOutputSizeBytes: 1,
  expectedOutputDownloadUrl: null,
  ...over,
});

const detailDto = (over: Record<string, unknown> = {}) => ({
  id: P,
  code: "TWO-SUM",
  title: "Two Sum",
  statementMd: "Cho mang",
  constraintsMd: "n <= 10",
  levelId: LEVEL_HARD,
  status: "PUBLISHED",
  limits: {
    timeLimitMs: 2000,
    memoryLimitMb: 256,
    maxOutputSizeKb: 64,
    maxSubmissionsPerHour: null,
  },
  topicIds: [TOPIC_1, TOPIC_2],
  tags: [{ id: "g1", name: "hash" }],
  referenceSolution: { language: "CPP", sourceCode: "int main(){}" },
  workedExamples: [
    { id: EX_ID, position: 1, input: "i", output: "o", explanationMd: null },
  ],
  testcases: [testcaseDto()],
  testcaseSummary: { total: 1, approved: 1, sample: 1, hidden: 0 },
  spec: null,
  functionWrapperSupported: false,
  aiContext: { briefText: null, noFullCode: true, questionOnly: false },
  aiGeneration: { used: 1, limit: 2 },
  updatedAt: T0,
  duplicatedFromProblemId: null,
  readOnly: false,
  ...over,
});

/** Routes the reads by path; `detail` is what #33 answers. */
function reads(
  detail: unknown = detailDto(),
  stats: unknown = { submissionCount: 40, acRate: null, updatedAt: null },
) {
  get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/levels") return levelsDto;
    if (path === `${A}/${P}`) return detail;
    if (path === `${A}/${P}/stats`) {
      if (stats instanceof Error) throw stats;
      return stats;
    }
    throw new Error(`unexpected GET ${path}`);
  });
}

const server: ServerState = {
  updatedAt: T0,
  readOnly: false,
  levelIdByCode: { EASY: LEVEL_EASY, HARD: LEVEL_HARD },
  testcaseSummary: { total: 0, approved: 0, sample: 0, hidden: 0 },
  willCopy: null,
  duplicatedFromProblemId: null,
};

async function loaded(): Promise<ProblemDraft> {
  reads();
  return (await realLoadDraft(P)).draft;
}

beforeEach(() => {
  [get, post, put, patch, del].forEach((fn) => fn.mockReset());
});

describe("spec mapping", () => {
  it("an undeclared signature is sent as three nulls, never half declared", () => {
    const body = specBody(
      { ...emptySpec(), stdinFormat: "in", stdoutFormat: "out" },
      T0,
    );
    expect(body).toEqual({
      matchingStrategy: "EXACT",
      epsilonValue: null,
      stdinFormatMd: "in",
      stdoutFormatMd: "out",
      functionName: null,
      returnType: null,
      parameters: null,
      nameOverrides: null,
      expectedUpdatedAt: T0,
    });
  });

  it("a declared signature keeps nested types, upper-cases the override languages and parses epsilon", () => {
    const spec = {
      ...emptySpec(),
      signature: {
        functionName: " two_sum ",
        returnType: { kind: "LIST" as const, of: { kind: "INT" as const } },
        parameters: [
          {
            id: "p1",
            name: "nums",
            type: {
              kind: "ARRAY" as const,
              of: { kind: "INT" as const },
              dimensions: 1,
            },
          },
        ],
        nameOverrides: { java: "twoSum", python: "  " },
      },
      matchingStrategy: "EPSILON" as const,
      epsilon: "0.001",
    };
    expect(specBody(spec, T0)).toMatchObject({
      functionName: "two_sum",
      returnType: { kind: "LIST", of: { kind: "INT" } },
      parameters: [
        {
          name: "nums",
          type: { kind: "ARRAY", of: { kind: "INT" }, dimensions: 1 },
        },
      ],
      nameOverrides: { JAVA: "twoSum" },
      epsilonValue: 0.001,
    });
  });

  it("an unreadable epsilon is sent as null so the server answers HRN-V20 itself", () => {
    expect(
      specBody(
        { ...emptySpec(), matchingStrategy: "EPSILON", epsilon: "abc" },
        T0,
      ).epsilonValue,
    ).toBeNull();
  });

  it("reads a spec back: unknown kind shows as OTHER, null signature is the empty one", () => {
    const spec = toSpec({
      matchingStrategy: "TRIMMED",
      epsilonValue: null,
      stdinFormatMd: "in",
      stdoutFormatMd: "out",
      functionName: "f",
      returnType: { kind: "GRAPH" },
      parameters: [{ name: "x", type: { kind: "INT" } }],
      nameOverrides: { CPP: "ff" },
    });
    expect(spec.signature.returnType.kind).toBe("OTHER");
    expect(spec.signature.nameOverrides).toEqual({ cpp: "ff" });
    expect(spec.signature.parameters[0]).toMatchObject({ id: "p1", name: "x" });
    expect(toSpec(null)).toEqual(emptySpec());
  });
});

describe("realLoadDraft (#33, #34, #14, #40)", () => {
  it("maps a stored problem field by field", async () => {
    reads();
    const record = await realLoadDraft(P);
    expect(get).toHaveBeenCalledWith(`${A}/${P}`);
    const { draft } = record;
    expect(draft).toMatchObject({
      title: "Two Sum",
      body: "Cho mang",
      constraints: "n <= 10",
      topic: TOPIC_1,
      otherTopicIds: [TOPIC_2],
      difficulty: "HARD",
      status: "published",
      limits: {
        timeLimitMs: 2000,
        memoryLimitMb: 256,
        outputLimitKb: 64,
        maxSubmissionsPerHour: null,
      },
      tags: ["hash"],
      solutionLanguage: "C++",
      solution: "int main(){}",
      aiBrief: "",
      aiGuards: { noFullCode: true, socraticOnly: false },
      solutionCheck: { ran: false, passed: 0, total: 0 },
    });
    expect(draft.examples).toEqual([
      { id: EX_ID, input: "i", output: "o", explanation: "" },
    ]);
    expect(draft.testcases[0]).toEqual({
      id: "tc-1",
      input: "1 2",
      expected: "3",
      visibility: "public",
      origin: "manual",
      approved: true,
      category: "boundaryLow",
    });
    expect(record.aiGeneration).toEqual({ used: 1, limit: 2 });
    expect(record.savedAt).toBe(T0);
    expect(record.server).toMatchObject({
      updatedAt: T0,
      readOnly: false,
      levelIdByCode: { EASY: LEVEL_EASY, HARD: LEVEL_HARD },
    });
  });

  it("keeps acRate null and a failed stats call as unknown, never as zero", async () => {
    reads(detailDto(), { submissionCount: 40, acRate: null, updatedAt: null });
    expect((await realLoadDraft(P)).stats).toEqual({
      submissionCount: 40,
      acRate: null,
    });
    reads(detailDto(), new ApiError("PRB-010", 404));
    expect((await realLoadDraft(P)).stats).toBeNull();
  });

  it("a side in object storage arrives empty and flagged, so it is never written back", async () => {
    reads(
      detailDto({
        testcases: [
          testcaseDto({
            input: null,
            inputStorage: "MINIO",
            expectedOutput: null,
            expectedOutputStorage: "MINIO",
            origin: "AI",
            approved: false,
            category: null,
            visibility: "HIDDEN",
          }),
        ],
      }),
    );
    const row = (await realLoadDraft(P)).draft.testcases[0]!;
    expect(row).toMatchObject({
      input: "",
      expected: "",
      visibility: "hidden",
      origin: "ai",
      approved: false,
      largeSides: ["input", "expected"],
    });
    expect(row.category).toBeUndefined();
  });

  it("read-only view with counts only: no testcase rows, the summary is kept, the flag is passed on", async () => {
    reads(
      detailDto({
        readOnly: true,
        testcases: [],
        referenceSolution: null,
        testcaseSummary: { total: 9, approved: 8, sample: 2, hidden: 7 },
      }),
    );
    const record = await realLoadDraft(P);
    expect(record.draft.testcases).toEqual([]);
    expect(record.draft.solution).toBe("");
    expect(record.server).toMatchObject({
      readOnly: true,
      testcaseSummary: { total: 9, approved: 8, sample: 2, hidden: 7 },
    });
  });

  it.each([
    ["PRB-010", 404],
    ["PRB-002", 403],
  ])(
    "rethrows %s so the screen can tell not-found from forbidden",
    async (code, status) => {
      get.mockImplementation(async (path: string) => {
        if (path === "/problem-bank/levels") return levelsDto;
        throw new ApiError(code, status);
      });
      await expect(realLoadDraft(P)).rejects.toMatchObject({ code, status });
    },
  );

  it("a duplicate loads #34: (bản sao) suffix, new example ids, no testcases, what will be copied", async () => {
    get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/levels") return levelsDto;
      if (path === `${A}/${P}/duplicate-source`)
        return {
          title: "Two Sum",
          statementMd: "S",
          constraintsMd: "",
          levelId: LEVEL_EASY,
          limits: detailDto().limits,
          topicIds: [TOPIC_1],
          tags: ["a"],
          referenceSolution: null,
          workedExamples: [
            {
              id: null,
              position: 1,
              input: "i",
              output: "o",
              explanationMd: "e",
            },
          ],
          willCopyOnSave: { testcaseCount: 12, hasSpec: true },
          duplicatedFromProblemId: P,
        };
      throw new Error(`unexpected GET ${path}`);
    });
    const record = await realLoadDraft(undefined, P);
    expect(record.draft.title).toBe("Two Sum (bản sao)");
    expect(record.draft.status).toBe("draft");
    expect(record.draft.difficulty).toBe("EASY");
    expect(record.draft.testcases).toEqual([]);
    expect(record.draft.examples[0]).toMatchObject({
      id: "ex-copy-1",
      explanation: "e",
    });
    expect(record.savedAt).toBeNull();
    expect(record.server).toMatchObject({
      updatedAt: null,
      willCopy: { testcaseCount: 12, hasSpec: true },
      duplicatedFromProblemId: P,
    });
  });

  it("a duplicate that is refused (A2, someone else's problem) rethrows PRB-003", async () => {
    get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/levels") return levelsDto;
      throw new ApiError("PRB-003", 403);
    });
    await expect(realLoadDraft(undefined, P)).rejects.toMatchObject({
      code: "PRB-003",
    });
  });

  it("a blank form starts on the first level with no topic and nothing invented to save", async () => {
    get.mockResolvedValue(levelsDto);
    const record = await realLoadDraft();
    expect(record.draft).toMatchObject({
      title: "",
      difficulty: "EASY",
      topic: "",
      status: "draft",
      testcases: [],
      examples: [],
    });
    expect(record.server?.updatedAt).toBeNull();
  });
});

describe("realSaveDraft: existing problem (#36 to #39)", () => {
  it("sends only the sections that changed and chains the lock value", async () => {
    const saved = await loaded();
    const draft: ProblemDraft = {
      ...saved,
      title: "Two Sum II",
      examples: [
        { id: "ex-new-1", input: "a", output: "b", explanation: "x" },
        ...saved.examples,
      ],
      aiBrief: "hint",
      spec: { ...saved.spec, stdinFormat: "line" },
      status: "draft",
    };
    put.mockImplementation(async (path: string) => {
      if (path.endsWith("/content")) return detailDto({ updatedAt: T1 });
      if (path.endsWith("/spec")) return { problemUpdatedAt: T2 };
      if (path.endsWith("/ai-context")) return { problemUpdatedAt: T3 };
      return { problemUpdatedAt: "2026-11-06T05:00:00Z" };
    });
    reads(detailDto({ updatedAt: "2026-11-06T05:00:00Z" }));

    const result = await realSaveDraft({
      draft,
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });

    expect(put.mock.calls.map((call) => call[0])).toEqual([
      `${A}/${P}/content`,
      `${A}/${P}/spec`,
      `${A}/${P}/ai-context`,
      `${A}/${P}/status`,
    ]);
    expect(put.mock.calls[0]![1]).toEqual({
      title: "Two Sum II",
      statementMd: "Cho mang",
      constraintsMd: "n <= 10",
      levelId: LEVEL_HARD,
      limits: {
        timeLimitMs: 2000,
        memoryLimitMb: 256,
        maxOutputSizeKb: 64,
        maxSubmissionsPerHour: null,
      },
      topicIds: [TOPIC_1, TOPIC_2],
      tags: ["hash"],
      referenceSolution: { language: "CPP", sourceCode: "int main(){}" },
      workedExamples: [
        { id: null, input: "a", output: "b", explanationMd: "x" },
        { id: EX_ID, input: "i", output: "o", explanationMd: null },
      ],
      expectedUpdatedAt: T0,
    });
    expect(put.mock.calls[1]![1]).toMatchObject({ expectedUpdatedAt: T1 });
    expect(put.mock.calls[2]![1]).toEqual({
      briefText: "hint",
      noFullCode: true,
      questionOnly: false,
      expectedUpdatedAt: T2,
    });
    expect(put.mock.calls[3]![1]).toEqual({
      status: "UNPUBLISHED",
      expectedUpdatedAt: T3,
    });
    expect(result.applied).toEqual(["content", "spec", "ai", "status"]);
    expect(result.error).toBeUndefined();
    expect(result.updatedAt).toBe("2026-11-06T05:00:00Z");
    expect(result.record?.draft.title).toBe("Two Sum");
  });

  it("an unchanged draft sends nothing and an empty AI note is stored as null", async () => {
    const saved = await loaded();
    expect(
      (
        await realSaveDraft({
          draft: saved,
          saved,
          problemId: P,
          server,
          updatedAt: T0,
        })
      ).applied,
    ).toEqual([]);
    expect(put).not.toHaveBeenCalled();
    put.mockResolvedValue({ problemUpdatedAt: T1 });
    await realSaveDraft({
      draft: {
        ...saved,
        aiBrief: "   ",
        aiGuards: { noFullCode: false, socraticOnly: false },
      },
      saved: { ...saved, aiBrief: "x" },
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(put.mock.calls[0]![1]).toMatchObject({
      briefText: null,
      noFullCode: false,
    });
  });

  it("clearing the reference solution sends null, a tag list is trimmed and de-duplicated", async () => {
    const saved = await loaded();
    put.mockResolvedValue(detailDto({ updatedAt: T1 }));
    await realSaveDraft({
      draft: { ...saved, solution: "  ", tags: [" a ", "a", ""] },
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(put.mock.calls[0]![1]).toMatchObject({
      referenceSolution: null,
      tags: ["a"],
    });
  });

  it("PRB-020 on the first step is returned, nothing is marked applied and nothing else is called", async () => {
    const saved = await loaded();
    const stale = new ApiError("PRB-020", 409, undefined, {
      currentUpdatedAt: T2,
    });
    put.mockRejectedValueOnce(stale);
    const result = await realSaveDraft({
      draft: { ...saved, title: "x", aiBrief: "y" },
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(result.error).toBe(stale);
    expect(result.applied).toEqual([]);
    expect(put).toHaveBeenCalledTimes(1);
    expect(result.record).toBeUndefined();
  });

  it("a spec the server refuses (PRB-036) keeps the content step as applied and carries the lock forward", async () => {
    const saved = await loaded();
    const refusal = new ApiError("PRB-036", 422, undefined, {
      violations: [{ code: "HRN-V22" }],
    });
    put.mockImplementation(async (path: string) => {
      if (path.endsWith("/content")) return detailDto({ updatedAt: T1 });
      throw refusal;
    });
    const result = await realSaveDraft({
      draft: {
        ...saved,
        title: "x",
        spec: { ...saved.spec, stdinFormat: "z" },
      },
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(result.applied).toEqual(["content"]);
    expect(result.error).toBe(refusal);
    expect(result.updatedAt).toBe(T1);
  });

  it("PRB-030 on publish is returned with the earlier sections applied", async () => {
    const saved = { ...(await loaded()), status: "draft" as const };
    const gate = new ApiError("PRB-030", 422, undefined, {
      items: [{ code: "MIN_APPROVED_TESTCASES", met: false }],
    });
    put.mockRejectedValueOnce(gate);
    const result = await realSaveDraft({
      draft: { ...saved, status: "published" },
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(put).toHaveBeenCalledWith(`${A}/${P}/status`, {
      status: "PUBLISHED",
      expectedUpdatedAt: T0,
    });
    expect(result.error).toBe(gate);
    expect(result.applied).toEqual([]);
  });

  it("a level deleted meanwhile is PRB-039 before any request", async () => {
    const saved = await loaded();
    const result = await realSaveDraft({
      draft: { ...saved, title: "x", difficulty: "GONE" },
      saved,
      problemId: P,
      server,
      updatedAt: T0,
    });
    expect(result.error).toMatchObject({ code: "PRB-039", status: 422 });
    expect(put).not.toHaveBeenCalled();
  });
});

describe("realSaveDraft: first save (#35)", () => {
  const blank = async () =>
    (
      await (async () => {
        get.mockResolvedValue(levelsDto);
        return realLoadDraft();
      })()
    ).draft;

  it("creates the problem, writes the rows added before the first save, then adopts what the server holds", async () => {
    const base = await blank();
    const draft: ProblemDraft = {
      ...base,
      title: " New ",
      body: "b",
      topic: TOPIC_1,
      testcases: [
        {
          id: "tc-new-1",
          input: "1",
          expected: "2",
          visibility: "hidden",
          origin: "manual",
          approved: true,
        },
      ],
    };
    post.mockImplementation(async (path: string) =>
      path === A
        ? detailDto({ updatedAt: T1 })
        : { testcase: testcaseDto({ id: "srv" }), problemUpdatedAt: T2 },
    );
    reads(detailDto({ updatedAt: T2 }));
    const result = await realSaveDraft({
      draft,
      saved: base,
      server: { ...server, updatedAt: null },
      updatedAt: null,
    });
    expect(post.mock.calls[0]![0]).toBe(A);
    expect(post.mock.calls[0]![1]).toMatchObject({
      title: "New",
      levelId: LEVEL_EASY,
      topicIds: [TOPIC_1],
      duplicatedFromProblemId: null,
      workedExamples: [],
    });
    expect(post.mock.calls[1]).toEqual([
      `${A}/${P}/testcases`,
      { visibility: "HIDDEN", input: "1", expectedOutput: "2" },
    ]);
    expect(result.createdId).toBe(P);
    expect(result.applied).toEqual(["content"]);
    expect(result.updatedAt).toBe(T2);
  });

  it("a duplicate sends its source id so the server copies the testcases and spec itself", async () => {
    const base = await blank();
    post.mockResolvedValue(detailDto({ updatedAt: T1 }));
    reads();
    await realSaveDraft({
      draft: { ...base, title: "t", body: "b" },
      saved: base,
      server: { ...server, updatedAt: null, duplicatedFromProblemId: P },
      updatedAt: null,
    });
    expect(post.mock.calls[0]![1]).toMatchObject({
      duplicatedFromProblemId: P,
    });
    expect(post).toHaveBeenCalledTimes(1);
  });

  it("if a later step fails the created id and the server's testcase rows still come back", async () => {
    const base = await blank();
    const refusal = new ApiError("PRB-036", 422);
    post.mockResolvedValue(detailDto({ updatedAt: T1 }));
    put.mockRejectedValue(refusal);
    reads();
    const result = await realSaveDraft({
      draft: {
        ...base,
        title: "t",
        body: "b",
        spec: { ...base.spec, stdinFormat: "x" },
      },
      saved: base,
      server: { ...server, updatedAt: null },
      updatedAt: null,
    });
    expect(result.createdId).toBe(P);
    expect(result.applied).toEqual(["content"]);
    expect(result.error).toBe(refusal);
    expect(result.record?.draft.testcases[0]?.id).toBe("tc-1");
  });
});

describe("testcases (#41 to #44)", () => {
  it("creates with the wire enum and reads the server's row back", async () => {
    post.mockResolvedValue({
      testcase: testcaseDto({ id: "n", visibility: "HIDDEN", category: null }),
      problemUpdatedAt: T1,
    });
    const result = await realCreateTestcase(P, {
      input: "i",
      expected: "o",
      visibility: "hidden",
    });
    expect(post).toHaveBeenCalledWith(`${A}/${P}/testcases`, {
      visibility: "HIDDEN",
      input: "i",
      expectedOutput: "o",
    });
    expect(result).toMatchObject({
      updatedAt: T1,
      testcase: { id: "n", visibility: "hidden" },
    });
  });

  it.each(["PRB-046", "PRB-048", "PRB-003"])(
    "rethrows %s from a create",
    async (code) => {
      post.mockRejectedValue(new ApiError(code, 422));
      await expect(
        realCreateTestcase(P, {
          input: "i",
          expected: "o",
          visibility: "public",
        }),
      ).rejects.toMatchObject({ code });
    },
  );

  it("patches only the keys given: approve sends approved true and nothing else", async () => {
    patch.mockResolvedValue({ testcase: testcaseDto(), problemUpdatedAt: T1 });
    await realUpdateTestcase(P, "tc-1", { approved: true });
    expect(patch).toHaveBeenCalledWith(`${A}/${P}/testcases/tc-1`, {
      approved: true,
    });
    await realUpdateTestcase(P, "tc-1", {
      input: "x",
      expected: "y",
      visibility: "public",
    });
    expect(patch).toHaveBeenLastCalledWith(`${A}/${P}/testcases/tc-1`, {
      input: "x",
      expectedOutput: "y",
      visibility: "SAMPLE",
    });
  });

  it.each([
    ["PRB-011", 404],
    ["PRB-054", 422],
    ["PRB-048", 422],
  ])("rethrows %s from an update", async (code, status) => {
    patch.mockRejectedValue(new ApiError(code, status));
    await expect(
      realUpdateTestcase(P, "tc-1", { visibility: "hidden" }),
    ).rejects.toMatchObject({ code });
  });

  it("deletes and returns the new lock value; PRB-054 and PRB-011 are rethrown", async () => {
    del.mockResolvedValue({
      currentTestcaseSetVersion: 2,
      problemUpdatedAt: T1,
    });
    expect(await realDeleteTestcase(P, "tc-1")).toEqual({ updatedAt: T1 });
    expect(del).toHaveBeenCalledWith(`${A}/${P}/testcases/tc-1`);
    del.mockRejectedValue(new ApiError("PRB-054", 422));
    await expect(realDeleteTestcase(P, "tc-1")).rejects.toMatchObject({
      code: "PRB-054",
    });
  });

  it("reorders with the full id list; PRB-049 is rethrown", async () => {
    put.mockResolvedValue({
      items: [],
      problemUpdatedAt: T1,
      currentTestcaseSetVersion: 1,
    });
    expect(await realReorderTestcases(P, ["b", "a"])).toEqual({
      updatedAt: T1,
    });
    expect(put).toHaveBeenCalledWith(`${A}/${P}/testcases/order`, {
      orderedIds: ["b", "a"],
    });
    put.mockRejectedValue(new ApiError("PRB-049", 422));
    await expect(realReorderTestcases(P, ["a"])).rejects.toMatchObject({
      code: "PRB-049",
    });
  });
});

describe("bulk upload (#45, #46)", () => {
  const pair = (key: string) => ({
    clientKey: key,
    input: new Blob(["abc"]),
    expected: new Blob(["de"]),
    visibility: "public" as const,
  });

  it("asks for URLs with the sizes, PUTs every file with no credentials, then confirms the upload id", async () => {
    post.mockImplementation(async (path: string) =>
      path.endsWith("bulk-upload-url")
        ? {
            uploadId: "u1",
            items: [
              {
                clientKey: "a",
                input: { putUrl: "https://s/in-a" },
                expectedOutput: { putUrl: "https://s/out-a" },
              },
            ],
          }
        : { added: [testcaseDto({ id: "n1" })], problemUpdatedAt: T1 },
    );
    const fetchMock = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal("fetch", fetchMock);
    const result = await realBulkUpload(P, [pair("a")]);
    expect(post.mock.calls[0]).toEqual([
      `${A}/${P}/testcases/bulk-upload-url`,
      {
        items: [
          {
            clientKey: "a",
            inputSizeBytes: 3,
            expectedOutputSizeBytes: 2,
            visibility: "SAMPLE",
          },
        ],
      },
    ]);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0]![0]).toBe("https://s/in-a");
    expect(fetchMock.mock.calls[0]![1]).toMatchObject({
      method: "PUT",
      credentials: "omit",
    });
    expect(post.mock.calls[1]).toEqual([
      `${A}/${P}/testcases/bulk-upload/confirm`,
      { uploadId: "u1" },
    ]);
    expect(result.added[0]!.id).toBe("n1");
    expect(result.updatedAt).toBe(T1);
    vi.unstubAllGlobals();
  });

  it("a failed PUT stops before the confirm call: nothing is added half way", async () => {
    post.mockResolvedValueOnce({
      uploadId: "u1",
      items: [
        {
          clientKey: "a",
          input: { putUrl: "https://s/1" },
          expectedOutput: { putUrl: "https://s/2" },
        },
      ],
    });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false }));
    await expect(realBulkUpload(P, [pair("a")])).rejects.toMatchObject({
      code: "PRB-052",
    });
    expect(post).toHaveBeenCalledTimes(1);
    vi.unstubAllGlobals();
  });

  it.each([
    ["PRB-045", 422],
    ["PRB-044", 422],
    ["PRB-047", 422],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realBulkUpload(P, [pair("a")])).rejects.toMatchObject({
      code,
    });
  });
});

describe("run the reference solution (#47)", () => {
  it("sends the editor's code with the wire language and returns counts and the first failure by position", async () => {
    post.mockResolvedValue({
      compileError: null,
      summary: { passed: 1, total: 3, allPass: false },
      results: [
        { position: 3, verdict: "FAIL", failure: "TIME_LIMIT_EXCEEDED" },
        { position: 1, verdict: "PASS", failure: null },
        { position: 2, verdict: "FAIL", failure: "WRONG_ANSWER" },
      ],
    });
    const run = await realRunReferenceSolution(P, {
      language: "C++",
      sourceCode: "code",
    });
    expect(post).toHaveBeenCalledWith(`${A}/${P}/reference-solution/run`, {
      language: "CPP",
      sourceCode: "code",
    });
    expect(run).toEqual({
      compileError: null,
      passed: 1,
      total: 3,
      allPass: false,
      firstFailure: { position: 2, failure: "WRONG_ANSWER" },
    });
  });

  it("a compile error and an empty set are reported as such", async () => {
    post.mockResolvedValueOnce({
      compileError: "boom",
      summary: { passed: 0, total: 2, allPass: false },
      results: [],
    });
    expect(
      (await realRunReferenceSolution(P, { language: "Java", sourceCode: "x" }))
        .compileError,
    ).toBe("boom");
    post.mockResolvedValueOnce({
      compileError: null,
      summary: { passed: 0, total: 0, allPass: false },
      results: [],
    });
    expect(
      (
        await realRunReferenceSolution(P, {
          language: "Python",
          sourceCode: "x",
        })
      ).total,
    ).toBe(0);
  });

  it.each([
    ["PRB-040", 422],
    ["PRB-055", 429],
    ["PRB-050", 503],
    ["PRB-011", 404],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(
      realRunReferenceSolution(P, { language: "Java", sourceCode: "x" }),
    ).rejects.toMatchObject({ code, status });
  });
});
