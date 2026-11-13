import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const downloadPost = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    downloadPost: (...args: unknown[]) => downloadPost(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realAttentionList,
  realBulkDelete,
  realBulkDifficulty,
  realBulkPublish,
  realExportProblems,
  realListAdminProblems,
  realTopicDistribution,
} from "./real-problem-management";
import type { AdminListParams } from "../model/types";

const BASE = "/problem-bank/authoring/problems";

const params: AdminListParams = {
  q: "",
  level: "all",
  status: "all",
  sort: "edited",
  dir: "desc",
  page: 1,
  pageSize: 8,
};

const dto = {
  id: "p1",
  code: "P-0001",
  title: "Two Sum",
  topicNames: ["Mảng", "Bảng băm"],
  levelId: "l1",
  levelCode: "EASY",
  levelName: "Dễ",
  status: "PUBLISHED",
  submissionCount: 12,
  assignedClassCount: 2,
  acRate: null,
  testcaseCount: 9,
  updatedAt: "2026-11-05T03:00:00Z",
};

const emptyList = {
  items: [],
  page: { number: 1, size: 8, totalItems: 0, totalPages: 1 },
  totals: { total: 0, published: 0 },
};

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  downloadPost.mockReset();
});

describe("realListAdminProblems (#24)", () => {
  it("sends the default query and maps a row, keeping a missing AC rate as null", async () => {
    get.mockResolvedValue({
      items: [dto],
      page: { number: 1, size: 8, totalItems: 1, totalPages: 1 },
      totals: { total: 30, published: 20 },
    });
    const list = await realListAdminProblems(params, undefined);
    expect(get).toHaveBeenCalledWith(BASE, {
      sort: "updatedAt",
      dir: "desc",
      page: 1,
      pageSize: 8,
    });
    expect(list.totals).toEqual({ total: 30, published: 20 });
    expect(list.page.totalItems).toBe(1);
    expect(list.items[0]).toEqual({
      id: "p1",
      code: "P-0001",
      title: "Two Sum",
      topics: ["Mảng", "Bảng băm"],
      difficulty: "EASY",
      status: "published",
      submissionCount: 12,
      assignedClassCount: 2,
      acceptedRate: null,
      testcaseCount: 9,
      updatedAt: "2026-11-05T03:00:00Z",
    });
  });

  it("turns the screen's filters into the server's: level id, status enum, trimmed text, sort field", async () => {
    get.mockResolvedValue({
      items: [{ ...dto, status: "UNPUBLISHED", acRate: 55.5 }],
      page: { number: 2, size: 20, totalItems: 21, totalPages: 2 },
      totals: { total: 21, published: 0 },
    });
    const list = await realListAdminProblems(
      {
        q: "  coin ",
        level: "EASY",
        status: "draft",
        sort: "difficulty",
        dir: "asc",
        page: 2,
        pageSize: 20,
      },
      "level-uuid",
    );
    expect(get).toHaveBeenCalledWith(BASE, {
      q: "coin",
      level: "level-uuid",
      status: "UNPUBLISHED",
      sort: "level",
      dir: "asc",
      page: 2,
      pageSize: 20,
    });
    expect(list.items[0]).toMatchObject({
      status: "draft",
      acceptedRate: 55.5,
    });
  });

  it.each([
    ["submissions", "submissionCount"],
    ["accepted", "acRate"],
    ["code", "code"],
    ["title", "title"],
    ["status", "status"],
  ] as const)("maps the sort key %s to %s", async (key, field) => {
    get.mockResolvedValue(emptyList);
    await realListAdminProblems({ ...params, sort: key }, undefined);
    expect(get.mock.calls[0]![1]).toMatchObject({ sort: field });
  });

  it("never sends a level it cannot resolve", async () => {
    get.mockResolvedValue(emptyList);
    await realListAdminProblems({ ...params, level: "GONE" }, undefined);
    expect(get.mock.calls[0]![1]).not.toHaveProperty("level");
  });

  it.each([
    ["PRB-001", 400],
    ["PRB-002", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(
      realListAdminProblems(params, undefined),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("the side blocks (#25, #26)", () => {
  it("maps the topic distribution and keeps the denominator", async () => {
    get.mockResolvedValue({
      totalProblemsWithTopic: 10,
      totalTopicsUsed: 2,
      items: [
        { topicId: "t1", topicName: "Mảng", count: 6, percent: 60.0 },
        { topicId: "t2", topicName: "Cây", count: 4, percent: 40.0 },
      ],
    });
    expect(await realTopicDistribution()).toEqual({
      totalProblemsWithTopic: 10,
      totalTopicsUsed: 2,
      items: [
        { topicName: "Mảng", count: 6, percent: 60 },
        { topicName: "Cây", count: 4, percent: 40 },
      ],
    });
    expect(get).toHaveBeenCalledWith(`${BASE}/topic-distribution`);
  });

  it("maps the four attention rules and drops a code it does not know", async () => {
    get.mockResolvedValue({
      items: [
        { ruleCode: "NO_TESTCASE", count: 1 },
        { ruleCode: "LOW_AC", count: 2 },
        { ruleCode: "HIDDEN_FROM_LEARNERS", count: 0 },
        { ruleCode: "STALE_DRAFT", count: 3 },
        { ruleCode: "FUTURE_RULE", count: 9 },
      ],
    });
    expect(await realAttentionList()).toEqual([
      { ruleCode: "noTestcase", count: 1 },
      { ruleCode: "lowAcceptRate", count: 2 },
      { ruleCode: "hiddenFromLearners", count: 0 },
      { ruleCode: "staleDraft", count: 3 },
    ]);
  });
});

describe("bulk actions (#27, #28, #30, #32)", () => {
  const answer = {
    succeededIds: ["p1"],
    failedItems: [
      { id: "p2", code: "PRB-030", message: "checklist", details: { a: 1 } },
    ],
  };

  it("publish sends only the ids and keeps each problem's own failure code", async () => {
    post.mockResolvedValue(answer);
    const result = await realBulkPublish(["p1", "p2"]);
    expect(post).toHaveBeenCalledWith(`${BASE}/bulk/publish`, {
      problemIds: ["p1", "p2"],
    });
    expect(result).toEqual({
      succeededIds: ["p1"],
      failedItems: [{ id: "p2", code: "PRB-030" }],
    });
  });

  it("delete sends only the ids", async () => {
    post.mockResolvedValue({ succeededIds: ["p1"], failedItems: [] });
    await realBulkDelete(["p1"]);
    expect(post).toHaveBeenCalledWith(`${BASE}/bulk/delete`, {
      problemIds: ["p1"],
    });
  });

  it("difficulty sends the ids and the level id", async () => {
    post.mockResolvedValue({ succeededIds: ["p1"], failedItems: [] });
    await realBulkDifficulty(["p1"], "l9");
    expect(post).toHaveBeenCalledWith(`${BASE}/bulk/difficulty`, {
      problemIds: ["p1"],
      levelId: "l9",
    });
  });

  it("difficulty without a resolvable level fails as the server would (PRB-039) and calls nothing", async () => {
    await expect(realBulkDifficulty(["p1"], undefined)).rejects.toMatchObject({
      code: "PRB-039",
    });
    expect(post).not.toHaveBeenCalled();
  });

  it("a whole-batch rejection stays an ApiError", async () => {
    post.mockRejectedValue(new ApiError("PRB-039", 422));
    await expect(realBulkDifficulty(["p1"], "gone")).rejects.toMatchObject({
      code: "PRB-039",
      status: 422,
    });
  });

  it("export posts the selected ids and hands back the file and its name", async () => {
    const blob = new Blob(["x"]);
    downloadPost.mockResolvedValue({ blob, filename: "problems-20261106.csv" });
    const file = await realExportProblems(["p1", "p2"]);
    expect(downloadPost).toHaveBeenCalledWith(`${BASE}/export`, {
      problemIds: ["p1", "p2"],
    });
    expect(file.filename).toBe("problems-20261106.csv");
    expect(file.blob).toBe(blob);
  });
});
