import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realGetDailyUsage,
  realListTopProblems,
  realListTopUsers,
  realListUsageAlerts,
  realMarkAlertReviewed,
} from "./real-usage";

const ID = "11111111-1111-4111-8111-111111111111";
const BASE = "/ai/admin/usage";

beforeEach(() => {
  get.mockReset();
  post.mockReset();
});

describe("realGetDailyUsage (F5 #27)", () => {
  it.each([
    ["7d", 7],
    ["14d", 14],
    ["30d", 30],
  ] as const)("sends rangeDays for %s", async (range, rangeDays) => {
    get.mockResolvedValue({ rangeDays, points: [] });
    await realGetDailyUsage(range);
    expect(get).toHaveBeenCalledWith(`${BASE}/daily`, { rangeDays });
  });

  it("maps the four feature codes and keeps the server order", async () => {
    get.mockResolvedValue({
      rangeDays: 7,
      points: [
        {
          date: "2026-11-10",
          tokensByFeature: {
            SOLUTION_REVIEW: 1,
            MOCK_INTERVIEW: 2,
            GENERATE_TESTCASE: 3,
            PRACTICE_FEEDBACK: 4,
          },
        },
        { date: "2026-11-11", tokensByFeature: { SOLUTION_REVIEW: 9 } },
      ],
    });
    await expect(realGetDailyUsage("7d")).resolves.toEqual([
      {
        date: "2026-11-10",
        values: { review: 1, interview: 2, testcase: 3, practice: 4 },
      },
      {
        date: "2026-11-11",
        values: { review: 9, interview: 0, testcase: 0, practice: 0 },
      },
    ]);
  });

  it.each([
    ["AIR-V19", 400],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realGetDailyUsage("7d")).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realListUsageAlerts (F5 #28)", () => {
  it("maps a BUDGET and an ANOMALY alert, ratio parsed to a number", async () => {
    get.mockResolvedValue({
      items: [
        {
          alertType: "BUDGET",
          alertId: null,
          status: null,
          callCount: null,
          ratio: null,
          displayName: null,
          projectedRunOutDate: "2026-11-28",
          usedPercent: 74,
        },
        {
          alertType: "ANOMALY",
          alertId: ID,
          status: "OPEN",
          callCount: 214,
          ratio: "7.25",
          displayName: "An",
          userId: "u",
          windowStart: "2026-11-11T00:00:00Z",
        },
      ],
    });
    await expect(realListUsageAlerts()).resolves.toEqual([
      { kind: "budget" },
      {
        kind: "anomaly",
        alertId: ID,
        displayName: "An",
        callCount: 214,
        ratio: 7.25,
      },
    ]);
    expect(get).toHaveBeenCalledWith(`${BASE}/alerts`);
  });

  it("keeps a null display name", async () => {
    get.mockResolvedValue({
      items: [
        {
          alertType: "ANOMALY",
          alertId: ID,
          callCount: 5,
          ratio: 3,
          displayName: null,
        },
      ],
    });
    const [alert] = await realListUsageAlerts();
    expect(alert).toMatchObject({ kind: "anomaly", displayName: null });
  });

  it("rethrows AIR-301", async () => {
    get.mockRejectedValue(new ApiError("AIR-301", 403));
    await expect(realListUsageAlerts()).rejects.toMatchObject({
      code: "AIR-301",
    });
  });
});

describe("realMarkAlertReviewed (F5 #29)", () => {
  it("POSTs to the alert's review path without a body", async () => {
    post.mockResolvedValue({ alertId: ID, status: "REVIEWED" });
    await expect(realMarkAlertReviewed(ID)).resolves.toBeUndefined();
    expect(post).toHaveBeenCalledWith(`${BASE}/alerts/${ID}/review`);
  });

  it.each([
    ["AIR-120", 404],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realMarkAlertReviewed(ID)).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realListTopUsers (F5 #30)", () => {
  it("maps rows and keeps null name and null cost as null", async () => {
    get.mockResolvedValue({
      items: [
        {
          userId: "a",
          displayName: "An",
          tokens: 10,
          calls: 2,
          costUsd: "1.5",
        },
        { userId: "b", displayName: null, tokens: 5, calls: 1, costUsd: null },
      ],
    });
    await expect(realListTopUsers()).resolves.toEqual([
      { userId: "a", name: "An", tokens: 10, calls: 2, costUsd: 1.5 },
      { userId: "b", name: null, tokens: 5, calls: 1, costUsd: null },
    ]);
    expect(get).toHaveBeenCalledWith(`${BASE}/top-users`);
  });

  it.each([
    ["AIR-V02", 400],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realListTopUsers()).rejects.toMatchObject({ code, status });
  });
});

describe("realListTopProblems (F5 #31)", () => {
  it("maps rows, with null title and level when problem-bank is down", async () => {
    get.mockResolvedValue({
      items: [
        {
          problemId: "p1",
          problemTitle: "Two Sum",
          levelCode: "EASY",
          levelDisplayName: "Dễ",
          tokens: 100,
          calls: 4,
          avgTokensPerCall: 25,
        },
        {
          problemId: "p2",
          problemTitle: null,
          levelCode: null,
          levelDisplayName: null,
          tokens: 50,
          calls: 2,
          avgTokensPerCall: 25,
        },
      ],
    });
    await expect(realListTopProblems()).resolves.toEqual([
      {
        problemId: "p1",
        title: "Two Sum",
        levelCode: "EASY",
        levelName: "Dễ",
        tokens: 100,
        calls: 4,
        avgTokensPerCall: 25,
      },
      {
        problemId: "p2",
        title: null,
        levelCode: null,
        levelName: null,
        tokens: 50,
        calls: 2,
        avgTokensPerCall: 25,
      },
    ]);
    expect(get).toHaveBeenCalledWith(`${BASE}/top-problems`);
  });

  it.each([
    ["AIR-V02", 400],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    get.mockRejectedValue(new ApiError(code, status));
    await expect(realListTopProblems()).rejects.toMatchObject({ code, status });
  });
});
