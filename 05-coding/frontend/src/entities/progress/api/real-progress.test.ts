import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { ApiError } from "@/shared/api";
import {
  realGetDifficultyBreakdown,
  realGetProgressOverview,
  realGetTopicProgress,
} from "./real-progress";

const SUMMARY = {
  solvedProblemCount: 3,
  totalSubmissions: 10,
  acceptedCount: 5,
  acceptanceRatePercent: 63,
  firstTryAcceptedCount: 2,
  topLanguage: "PYTHON",
};
const LEVELS = {
  totalPublished: 40,
  solved: 3,
  byLevel: [
    {
      levelCode: "EASY",
      levelDisplayName: "De",
      sortOrder: 1,
      total: 30,
      solved: 3,
    },
    {
      levelCode: "HARD",
      levelDisplayName: "Kho",
      sortOrder: 2,
      total: 0,
      solved: 0,
    },
  ],
};
const METRICS = {
  streakDays: 4,
  lastSubmittedAt: "2026-11-06T10:00:00Z",
  dailyCounts: [],
};

/** Routes each path to a canned answer; a function answer is called (to throw). */
function serve(routes: Record<string, unknown>) {
  get.mockImplementation(async (path: string) => {
    const answer = routes[path];
    if (answer === undefined) throw new Error(`unexpected ${path}`);
    return typeof answer === "function" ? (answer as () => unknown)() : answer;
  });
}
const down = (code: string) => () => {
  throw new ApiError(code, 503, "down");
};

beforeEach(() => {
  get.mockReset();
});

describe("realGetProgressOverview", () => {
  it("takes the streak from F4 and the published total from F2 #13B", async () => {
    serve({
      "/identity/me/progress/summary": SUMMARY,
      "/judge/me/metrics": METRICS,
      "/problem-bank/me/levels/progress": LEVELS,
    });

    const overview = await realGetProgressOverview();

    expect(get).toHaveBeenCalledWith("/identity/me/progress/summary");
    expect(get).toHaveBeenCalledWith("/judge/me/metrics", { range: "all" });
    expect(overview).toEqual({
      solvedProblemCount: 3,
      totalPublishedProblemCount: 40,
      totalSubmissions: 10,
      acceptedCount: 5,
      acceptanceRatePercent: 63,
      currentStreakDays: 4,
      avgAttemptsPerSolved: null,
    });
  });

  it("keeps a null rate when no submission is eligible", async () => {
    serve({
      "/identity/me/progress/summary": {
        ...SUMMARY,
        solvedProblemCount: 0,
        acceptedCount: 0,
        acceptanceRatePercent: null,
      },
      "/judge/me/metrics": METRICS,
      "/problem-bank/me/levels/progress": LEVELS,
    });

    expect((await realGetProgressOverview()).acceptanceRatePercent).toBeNull();
  });

  it("degrades to null streak and total when F4 and F2 are down, F1 figures stay", async () => {
    serve({
      "/identity/me/progress/summary": SUMMARY,
      "/judge/me/metrics": down("JDG-507"),
      "/problem-bank/me/levels/progress": down("PRB-057"),
    });

    const overview = await realGetProgressOverview();

    expect(overview.solvedProblemCount).toBe(3);
    expect(overview.currentStreakDays).toBeNull();
    expect(overview.totalPublishedProblemCount).toBeNull();
  });

  it("rethrows an F1 error untouched", async () => {
    serve({
      "/identity/me/progress/summary": down("IDT-503"),
      "/judge/me/metrics": METRICS,
      "/problem-bank/me/levels/progress": LEVELS,
    });

    await expect(realGetProgressOverview()).rejects.toMatchObject({
      code: "IDT-503",
    });
  });
});

describe("realGetDifficultyBreakdown (F2 #13B)", () => {
  it("maps levelCode, solved and total, keeping levels without problems", async () => {
    serve({ "/problem-bank/me/levels/progress": LEVELS });

    expect(await realGetDifficultyBreakdown()).toEqual([
      { difficulty: "EASY", solvedCount: 3, totalCount: 30 },
      { difficulty: "HARD", solvedCount: 0, totalCount: 0 },
    ]);
    expect(get).toHaveBeenCalledWith("/problem-bank/me/levels/progress");
  });

  it("rethrows PRB-057 so the card shows its error state, not zeros", async () => {
    serve({ "/problem-bank/me/levels/progress": down("PRB-057") });

    await expect(realGetDifficultyBreakdown()).rejects.toMatchObject({
      code: "PRB-057",
    });
  });
});

describe("realGetTopicProgress (F2 #3 plus F4 metrics)", () => {
  const TOPICS = {
    items: [
      { topicId: "t1", topicName: "Mang", solved: 2, total: 5 },
      { topicId: "t2", topicName: "Do thi", solved: 0, total: 0 },
    ],
    solveStateAvailable: true,
  };

  it("joins solved/total with the AC rate (ratio to percent) and the last submission of the range", async () => {
    serve({
      "/problem-bank/topics/progress": TOPICS,
      "/judge/me/metrics": {
        ...METRICS,
        acRateByTopic: [
          {
            topicId: "t1",
            submissionCount: 3,
            acceptedCount: 2,
            acRate: 2 / 3,
            lastSubmittedAt: "2026-11-05T01:00:00Z",
          },
          {
            topicId: "t2",
            submissionCount: 0,
            acceptedCount: 0,
            acRate: null,
            lastSubmittedAt: null,
          },
        ],
      },
    });

    expect(await realGetTopicProgress("30d")).toEqual([
      {
        topicId: "t1",
        topicName: "Mang",
        solvedCount: 2,
        totalCount: 5,
        acRate: 67,
        lastSubmittedAt: "2026-11-05T01:00:00Z",
      },
      {
        topicId: "t2",
        topicName: "Do thi",
        solvedCount: 0,
        totalCount: 0,
        acRate: null,
        lastSubmittedAt: null,
      },
    ]);
    expect(get).toHaveBeenCalledWith("/judge/me/metrics", {
      range: "30d",
      topicIds: ["t1", "t2"],
    });
  });

  it("keeps solved/total and nulls the range figures when F4 fails", async () => {
    serve({
      "/problem-bank/topics/progress": TOPICS,
      "/judge/me/metrics": down("JDG-507"),
    });

    const rows = await realGetTopicProgress("7d");

    expect(rows.map((r) => [r.solvedCount, r.totalCount])).toEqual([
      [2, 5],
      [0, 0],
    ]);
    expect(
      rows.every((r) => r.acRate === null && r.lastSubmittedAt === null),
    ).toBe(true);
  });

  it("throws when F2 fails, never a fake zero", async () => {
    serve({ "/problem-bank/topics/progress": down("PRB-057") });

    await expect(realGetTopicProgress("all")).rejects.toMatchObject({
      code: "PRB-057",
    });
    expect(get).not.toHaveBeenCalledWith(
      "/judge/me/metrics",
      expect.anything(),
    );
  });

  it("throws when F2 says the solve state is unavailable", async () => {
    serve({
      "/problem-bank/topics/progress": {
        items: [{ topicId: "t1", topicName: "Mang", solved: null, total: 5 }],
        solveStateAvailable: false,
      },
    });

    await expect(realGetTopicProgress("all")).rejects.toMatchObject({
      code: "PRB-057",
    });
  });

  it("splits more than 50 topics into several F4 calls (JDG-V15)", async () => {
    const items = Array.from({ length: 51 }, (_, i) => ({
      topicId: `t${i}`,
      topicName: `Topic ${i}`,
      solved: 0,
      total: 1,
    }));
    serve({
      "/problem-bank/topics/progress": { items, solveStateAvailable: true },
      "/judge/me/metrics": { ...METRICS, acRateByTopic: [] },
    });

    await realGetTopicProgress("all");

    const sizes = get.mock.calls
      .filter(([path]) => path === "/judge/me/metrics")
      .map(([, params]) => (params as { topicIds: string[] }).topicIds.length);
    expect(sizes).toEqual([50, 1]);
  });
});
