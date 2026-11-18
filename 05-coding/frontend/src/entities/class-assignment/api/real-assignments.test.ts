// Real calls of INS0202 (F2 #49 to #53): paths, query params, mapping, and every error code is rethrown.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const del = vi.fn();
vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...a: unknown[]) => get(...a),
    post: (...a: unknown[]) => post(...a),
    delete: (...a: unknown[]) => del(...a),
  },
}));

import {
  realAssign,
  realAssignmentSummary,
  realListAssignable,
  realListAssignments,
  realRemoveAssignment,
  realTopAssignedProblems,
} from "./real-assignments";

const B = "/problem-bank/instructor";
const page = { number: 2, size: 20, totalItems: 21, totalPages: 2 };

beforeEach(() => {
  [get, post, del].forEach((fn) => fn.mockReset());
});

describe("realListAssignments (#49)", () => {
  it("asks for the All tab with only the page, and maps a row; unknown figures stay null", async () => {
    get.mockResolvedValue({
      items: [
        {
          problemId: "p1",
          title: "Two Sum",
          topicNames: ["Mảng", "Bảng băm"],
          levelCode: "EASY",
          assignedClasses: [
            { classId: "c1", className: "K21" },
            { classId: "c2", className: "K22" },
          ],
          submissionCount: null,
          acRatePercent: null,
          withdrawn: true,
        },
      ],
      page,
    });
    const result = await realListAssignments({
      classId: "all",
      q: "  ",
      page: 2,
    });
    expect(get).toHaveBeenCalledWith(`${B}/class-assignments`, { page: 2 });
    expect(result.page).toEqual(page);
    expect(result.items[0]).toEqual({
      id: "p1",
      title: "Two Sum",
      topic: "Mảng",
      topics: ["Mảng", "Bảng băm"],
      difficulty: "EASY",
      assignedClassIds: ["c1", "c2"],
      assignedClassNames: ["K21", "K22"],
      submissionCount: null,
      acRate: null,
      withdrawn: true,
    });
  });

  it("sends the class and the trimmed search for a class tab", async () => {
    get.mockResolvedValue({ items: [], page });
    await realListAssignments({ classId: "c1", q: " sum ", page: 1 });
    expect(get).toHaveBeenCalledWith(`${B}/class-assignments`, {
      classId: "c1",
      q: "sum",
      page: 1,
    });
  });

  it.each([
    ["PRB-005", 403],
    ["PRB-002", 403],
    ["PRB-058", 503],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(
      realListAssignments({ classId: "c1", q: "", page: 1 }),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("realAssignmentSummary (#50)", () => {
  it("renames the count and keeps a null average as null", async () => {
    get.mockResolvedValue({
      assignedProblemCount: 3,
      classCount: 2,
      avgAcRatePercent: null,
    });
    expect(await realAssignmentSummary()).toEqual({
      assignedCount: 3,
      classCount: 2,
      avgAcRate: null,
    });
    expect(get).toHaveBeenCalledWith(`${B}/class-assignments/summary`);
  });
});

describe("realListAssignable (#51)", () => {
  it("maps the bank rows and sends the search and page", async () => {
    get.mockResolvedValue({
      items: [
        {
          problemId: "p9",
          code: "P-9",
          title: "Word Break",
          levelCode: "HARD",
          topicNames: [],
        },
      ],
      page,
    });
    const result = await realListAssignable("c1", " word ", 2);
    expect(get).toHaveBeenCalledWith(`${B}/classes/c1/assignable-problems`, {
      q: "word",
      page: 2,
    });
    expect(result.items).toEqual([
      { id: "p9", title: "Word Break", topic: "", difficulty: "HARD" },
    ]);
  });

  it("rethrows PRB-005", async () => {
    get.mockRejectedValue(new ApiError("PRB-005", 403));
    await expect(realListAssignable("c1", "", 1)).rejects.toMatchObject({
      code: "PRB-005",
    });
  });
});

describe("realAssign (#52)", () => {
  it("posts the ids and keeps new, skipped and refused apart", async () => {
    post.mockResolvedValue({
      succeededIds: ["a"],
      skippedIds: ["b"],
      failedItems: [{ id: "c", code: "PRB-037", message: "x" }],
    });
    expect(await realAssign("c1", ["a", "b", "c"])).toEqual({
      succeeded: ["a"],
      skipped: ["b"],
      failed: [{ id: "c", code: "PRB-037" }],
    });
    expect(post).toHaveBeenCalledWith(`${B}/classes/c1/assignments`, {
      problemIds: ["a", "b", "c"],
    });
  });

  it.each([
    ["PRB-001", 400],
    ["PRB-005", 403],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realAssign("c1", ["a"])).rejects.toMatchObject({ code });
  });
});

describe("realRemoveAssignment (#53)", () => {
  it("deletes that problem of that class", async () => {
    await realRemoveAssignment("c1", "p1");
    expect(del).toHaveBeenCalledWith(`${B}/classes/c1/assignments/p1`);
  });

  it.each([
    ["PRB-025", 409],
    ["PRB-005", 403],
  ])("rethrows %s", async (code, status) => {
    del.mockRejectedValue(new ApiError(code, status));
    await expect(realRemoveAssignment("c1", "p1")).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realTopAssignedProblems (#54)", () => {
  it("calls the top endpoint without parameters and maps the three fields", async () => {
    get.mockResolvedValue({
      items: [
        { problemId: "p1", title: "Two Sum", submissionCount: 9 },
        { problemId: "p2", title: "Anagram", submissionCount: 4 },
      ],
    });

    expect(await realTopAssignedProblems()).toEqual([
      { id: "p1", title: "Two Sum", submissionCount: 9 },
      { id: "p2", title: "Anagram", submissionCount: 4 },
    ]);
    expect(get).toHaveBeenCalledWith(`${B}/assigned-problems/top`);
  });

  it("returns an empty list as the server degraded answer (statistics source down)", async () => {
    get.mockResolvedValue({ items: [] });
    expect(await realTopAssignedProblems()).toEqual([]);
  });

  it("rethrows PRB-002 for a caller without the class permission", async () => {
    get.mockRejectedValue(new ApiError("PRB-002", 403));
    await expect(realTopAssignedProblems()).rejects.toMatchObject({
      code: "PRB-002",
      status: 403,
    });
  });
});
