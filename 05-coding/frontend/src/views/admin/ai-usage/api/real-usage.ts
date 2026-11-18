// Real calls behind ADM0302 (03-dd/api/F5_ai-review.md #27 to #31). The record is the wire:
// AiUsageController and the dto records UsageDaily, UsageAlertItem, TopUserItem, TopProblemItem.
// Paths are relative to /api/v1. Errors (AIR-120, AIR-301, AIR-V02, AIR-V19) are rethrown as `ApiError`.
// Dropped, sent but unread: alert windowStart/windowEnd/baselineAvg/userId/status, usedPercent and
// projectedRunOutDate of the budget alert (the screen reads them from #18).
import { api } from "@/shared/api";
import type { AiFeature } from "@/entities/ai-budget";
import type {
  DailyUsage,
  TopProblem,
  TopUser,
  UsageAlert,
  UsageRange,
} from "../model/types";

const BASE = "/ai/admin/usage";

const RANGE_DAYS: Record<UsageRange, number> = {
  "7d": 7,
  "14d": 14,
  "30d": 30,
};

type DailyDto = {
  points: {
    date: string;
    tokensByFeature: Record<string, number>;
  }[];
};

type AlertDto = {
  alertType: "BUDGET" | "ANOMALY";
  alertId: string | null;
  callCount: number | null;
  ratio: number | string | null;
  displayName: string | null;
};

type TopUserDto = {
  userId: string;
  displayName: string | null;
  tokens: number;
  calls: number;
  costUsd: number | string | null;
};

type TopProblemDto = {
  problemId: string;
  problemTitle: string | null;
  levelCode: string | null;
  levelDisplayName: string | null;
  tokens: number;
  calls: number;
  avgTokensPerCall: number;
};

const FEATURE_KEY: Record<string, AiFeature> = {
  SOLUTION_REVIEW: "review",
  MOCK_INTERVIEW: "interview",
  GENERATE_TESTCASE: "testcase",
  PRACTICE_FEEDBACK: "practice",
};

/** #27: oldest day first, every day present; `rangeDays` is always sent. */
export async function realGetDailyUsage(
  range: UsageRange,
): Promise<DailyUsage[]> {
  const body = await api.get<DailyDto>(`${BASE}/daily`, {
    rangeDays: RANGE_DAYS[range],
  });
  return body.points.map((point) => {
    const values: Record<AiFeature, number> = {
      review: 0,
      interview: 0,
      testcase: 0,
      practice: 0,
    };
    for (const [code, tokens] of Object.entries(point.tokensByFeature)) {
      const feature = FEATURE_KEY[code];
      if (feature) values[feature] = tokens;
    }
    return { date: point.date, values };
  });
}

/** #28: budget alert first (if any), then open anomaly alerts. */
export async function realListUsageAlerts(): Promise<UsageAlert[]> {
  const body = await api.get<{ items: AlertDto[] }>(`${BASE}/alerts`);
  return body.items.map((item): UsageAlert =>
    item.alertType === "BUDGET" || item.alertId === null
      ? { kind: "budget" }
      : {
          kind: "anomaly",
          alertId: item.alertId,
          displayName: item.displayName,
          callCount: item.callCount ?? 0,
          ratio: Number(item.ratio ?? 0),
        },
  );
}

/** #29: no body; marking an already reviewed alert answers 200 too. */
export async function realMarkAlertReviewed(alertId: string): Promise<void> {
  await api.post(`${BASE}/alerts/${alertId}/review`);
}

/** #30: last 30 days, server default of 5 rows. */
export async function realListTopUsers(): Promise<TopUser[]> {
  const body = await api.get<{ items: TopUserDto[] }>(`${BASE}/top-users`);
  return body.items.map((item) => ({
    userId: item.userId,
    name: item.displayName,
    tokens: item.tokens,
    calls: item.calls,
    costUsd: item.costUsd === null ? null : Number(item.costUsd),
  }));
}

/** #31: last 30 days, server default of 5 rows. */
export async function realListTopProblems(): Promise<TopProblem[]> {
  const body = await api.get<{ items: TopProblemDto[] }>(
    `${BASE}/top-problems`,
  );
  return body.items.map((item) => ({
    problemId: item.problemId,
    title: item.problemTitle,
    levelCode: item.levelCode,
    levelName: item.levelDisplayName,
    tokens: item.tokens,
    calls: item.calls,
    avgTokensPerCall: item.avgTokensPerCall,
  }));
}
