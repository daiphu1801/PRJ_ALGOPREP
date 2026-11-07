import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realListCatalog,
  realMyClassAssignments,
  realMyClassProblems,
  realMyInProgress,
  realRandomProblem,
  realSuggestedProblems,
  realTopicProgress,
  toSolveState,
} from "./real-student-catalog";
import type { CatalogParams } from "../model/catalog-types";

const params: CatalogParams = {
  q: "",
  status: "all",
  difficulty: "all",
  topicId: "all",
  sort: "difficulty",
  dir: "asc",
  page: 1,
  pageSize: 12,
};

const item = {
  id: "p1",
  code: "P-1",
  title: "Two Sum",
  topicNames: ["Mảng"],
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  acRate: null,
  functionWrapperSupported: false,
  solveState: "UNSOLVED",
};

const page = { number: 1, size: 12, totalItems: 1, totalPages: 1 };

beforeEach(() => {
  get.mockReset();
});

describe("toSolveState", () => {
  it("maps the server's three values and keeps unknown as null", () => {
    expect(toSolveState("SOLVED")).toBe("solved");
    expect(toSolveState("ATTEMPTED")).toBe("attempted");
    expect(toSolveState("UNSOLVED")).toBe("todo");
    expect(toSolveState(null)).toBeNull();
    expect(toSolveState("SOMETHING_NEW")).toBeNull();
  });
});

describe("realListCatalog (#1)", () => {
  it("sends the default query and maps a row; a missing rate stays null, the review badge is not asked for", async () => {
    get.mockResolvedValue({ items: [item], page, solveStateAvailable: true });
    const result = await realListCatalog(params);
    expect(get).toHaveBeenCalledWith("/problem-bank/problems", {
      sort: "level",
      dir: "asc",
      page: 1,
      pageSize: 12,
    });
    expect(result.items[0]).toEqual({
      id: "p1",
      code: "P-1",
      title: "Two Sum",
      topics: ["Mảng"],
      difficulty: "EASY",
      submissionModel: "stdioOnly",
      acRate: null,
      solveState: "todo",
      hasSolutionReview: null,
    });
    expect(result.page).toEqual(page);
    expect(result.solveStateAvailable).toBe(true);
  });

  it("turns every filter into the server's parameter", async () => {
    get.mockResolvedValue({ items: [], page, solveStateAvailable: true });
    await realListCatalog({
      q: "  coin ",
      status: "attempted",
      difficulty: "HARD",
      topicId: "t-9",
      sort: "acRate",
      dir: "desc",
      page: 3,
      pageSize: 24,
    });
    expect(get).toHaveBeenCalledWith("/problem-bank/problems", {
      q: "coin",
      status: "ATTEMPTED",
      difficulty: "HARD",
      topic: "t-9",
      sort: "acRate",
      dir: "desc",
      page: 3,
      pageSize: 24,
    });
  });

  it("degraded: a null solve state stays null and the flag says why", async () => {
    get.mockResolvedValue({
      items: [{ ...item, solveState: null, functionWrapperSupported: true }],
      page,
      solveStateAvailable: false,
    });
    const result = await realListCatalog(params);
    expect(result.solveStateAvailable).toBe(false);
    expect(result.items[0]).toMatchObject({
      solveState: null,
      submissionModel: "both",
    });
  });

  it("rethrows a failure for the screen to translate", async () => {
    get.mockRejectedValue(new ApiError("UNKNOWN_ERROR", 503));
    await expect(realListCatalog(params)).rejects.toMatchObject({
      status: 503,
    });
  });
});

describe("realRandomProblem (#2)", () => {
  it("sends the filters only, no sort or paging, and returns the id", async () => {
    get.mockResolvedValue({ problemId: "p7" });
    const id = await realRandomProblem({
      ...params,
      q: "x",
      status: "todo",
      topicId: "t1",
    });
    expect(get).toHaveBeenCalledWith("/problem-bank/problems/random", {
      q: "x",
      status: "UNSOLVED",
      topic: "t1",
    });
    expect(id).toBe("p7");
  });

  it("an empty set is null, not an error", async () => {
    get.mockResolvedValue({ problemId: null });
    expect(await realRandomProblem(params)).toBeNull();
  });
});

describe("realTopicProgress (#3)", () => {
  it("maps the chips and keeps an unknown solved count null", async () => {
    get.mockResolvedValue({
      items: [
        {
          topicId: "t1",
          topicName: "Mảng",
          sortOrder: 1,
          solved: null,
          total: 5,
        },
        { topicId: "t2", topicName: "Cây", sortOrder: 2, solved: 0, total: 0 },
      ],
      solveStateAvailable: false,
    });
    expect(await realTopicProgress()).toEqual({
      items: [
        { id: "t1", name: "Mảng", solved: null, total: 5 },
        { id: "t2", name: "Cây", solved: 0, total: 0 },
      ],
      solveStateAvailable: false,
    });
    expect(get).toHaveBeenCalledWith("/problem-bank/topics/progress");
  });
});

describe("realMyClassAssignments (#4)", () => {
  it("maps groups, items and a null solve state", async () => {
    get.mockResolvedValue({
      groups: [
        {
          classId: "c1",
          className: "Java K21",
          items: [
            {
              problemId: "p1",
              title: "Two Sum",
              solveState: "SOLVED",
              assignedAt: "2026-11-01T03:00:00Z",
            },
            {
              problemId: "p2",
              title: "Word Break",
              solveState: null,
              assignedAt: "2026-10-30T03:00:00Z",
            },
          ],
        },
      ],
    });
    expect(await realMyClassAssignments()).toEqual([
      {
        classId: "c1",
        className: "Java K21",
        items: [
          {
            problemId: "p1",
            title: "Two Sum",
            solveState: "solved",
            assignedAt: "2026-11-01T03:00:00Z",
          },
          {
            problemId: "p2",
            title: "Word Break",
            solveState: null,
            assignedAt: "2026-10-30T03:00:00Z",
          },
        ],
      },
    ]);
    expect(get).toHaveBeenCalledWith("/problem-bank/me/class-assignments");
  });
});

describe("realMyInProgress (F4 #8)", () => {
  const base = {
    problemId: "p1",
    problem: { title: "Word Break" },
    language: "PYTHON",
    status: "WRONG_ANSWER",
    passedCount: 6,
    totalCount: 10,
  };

  it("asks for the limit and maps the verdict, language and counts", async () => {
    get.mockResolvedValue({ items: [base] });
    expect(await realMyInProgress(3)).toEqual([
      {
        problemId: "p1",
        title: "Word Break",
        language: "python",
        lastVerdict: "wrongAnswer",
        passedCount: 6,
        totalCount: 10,
      },
    ]);
    expect(get).toHaveBeenCalledWith("/judge/me/in-progress-problems", {
      limit: 3,
    });
  });

  it("keeps a missing result as null instead of 0 / 0", async () => {
    get.mockResolvedValue({
      items: [
        { ...base, status: "PENDING", passedCount: null, totalCount: null },
      ],
    });
    const [card] = await realMyInProgress(3);
    expect(card).toMatchObject({
      lastVerdict: "pending",
      passedCount: null,
      totalCount: null,
    });
  });

  it("drops a card the server could not name, and one with a status or language it does not know", async () => {
    get.mockResolvedValue({
      items: [
        { ...base, problemId: "a", problem: null },
        { ...base, problemId: "b", status: "NEW_STATUS" },
        { ...base, problemId: "c", language: "RUST" },
        { ...base, problemId: "d" },
      ],
    });
    expect((await realMyInProgress(3)).map((card) => card.problemId)).toEqual([
      "d",
    ]);
  });
});

describe("realMyClassProblems (#13A)", () => {
  const page = { number: 2, size: 20, totalItems: 21, totalPages: 2 };
  const row = {
    classId: "c1",
    problemId: "p1",
    code: "P-1",
    title: "Two Sum",
    levelDisplayName: "Dễ",
    solveState: "UNSOLVED",
    assignedAt: "2026-11-02T03:00:00Z",
    withdrawn: false,
  };

  it("asks for one class and one page, maps the solve state and keeps meta as sent", async () => {
    get.mockResolvedValue({
      items: [
        row,
        { ...row, problemId: "p2", solveState: "SOLVED", withdrawn: true },
      ],
      page,
      meta: { totalAssigned: 21, solvedCount: null },
    });
    const result = await realMyClassProblems("c1", 2);
    expect(get).toHaveBeenCalledWith("/problem-bank/me/class-problems", {
      classId: "c1",
      page: 2,
    });
    expect(
      result.items.map((item) => [item.solveState, item.withdrawn]),
    ).toEqual([
      ["todo", false],
      ["solved", true],
    ]);
    expect(result.page).toEqual(page);
    expect(result.meta).toEqual({ totalAssigned: 21, solvedCount: null });
  });

  it("drops a row with a solve state it does not know instead of calling it todo", async () => {
    get.mockResolvedValue({
      items: [{ ...row, solveState: "WEIRD" }, row],
      page,
      meta: { totalAssigned: 2, solvedCount: 0 },
    });
    expect((await realMyClassProblems("c1", 1)).items).toHaveLength(1);
  });

  it.each([
    ["PRB-015", 404],
    ["PRB-057", 503],
    ["PRB-058", 503],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realMyClassProblems("c1", 1)).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realSuggestedProblems (#12)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  const row = {
    problemId: "p9",
    code: "MERGE",
    title: "Merge Intervals",
    levelCode: "MEDIUM",
    levelDisplayName: "Trung bình",
    topicName: "Mảng",
    acRate: null,
    solveState: "UNSOLVED",
    functionWrapperSupported: false,
  };

  it("sends no filter for 'all' and maps the rows, keeping an unknown AC rate null", async () => {
    get.mockResolvedValue({
      items: [row, { ...row, problemId: "p10", topicName: null, solveState: null }],
      topicScores: [],
      rankingAvailable: true,
    });

    const rows = await realSuggestedProblems("all", "all");

    expect(get).toHaveBeenCalledWith("/problem-bank/suggested-problems", {});
    expect(rows[0]).toEqual({
      id: "p9",
      code: "MERGE",
      title: "Merge Intervals",
      topics: ["Mảng"],
      difficulty: "MEDIUM",
      submissionModel: "stdioOnly",
      acRate: null,
      solveState: "todo",
      hasSolutionReview: null,
    });
    // A row with no topic and no solve state stays unknown, never "todo".
    expect(rows[1]).toMatchObject({ topics: [], solveState: null });
  });

  it("asks the server to filter by difficulty and topic", async () => {
    get.mockResolvedValue({ items: [] });

    await realSuggestedProblems("HARD", "t-1");

    expect(get).toHaveBeenCalledWith("/problem-bank/suggested-problems", {
      difficulty: "HARD",
      topic: "t-1",
    });
  });
});
