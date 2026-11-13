// MOCK-MODE data of ADM0302 (the real source is F5 #27 to #31, api/real-usage.ts).
// Values from 09-layoutBase/Admin - Token AI.dc.html:450-525, with the cut "Gợi ý" feature removed
// (DEC-2026-0831-remove-tiered-hints-ai-config).
import { mockHasModelPrices } from "@/entities/ai-budget";
import type {
  DailyUsage,
  TopProblem,
  TopUser,
  UsageAlert,
  UsageRange,
} from "../../model/types";

/**
 * The mockup generates its bars from `Math.sin(i)` at render time (dc.html:458-472). Reproduced
 * deterministically here — the same shape (weekends dip to 58%) without a random source, so two
 * renders and two test runs always agree.
 */
function buildDays(count: number): DailyUsage[] {
  const days: DailyUsage[] = [];
  for (let index = 0; index < count; index += 1) {
    const date = new Date(2026, 7, 22 - (count - 1 - index));
    const weekday = date.getDay();
    const weekendFactor = weekday === 0 || weekday === 6 ? 0.58 : 1;
    const jitter = 0.72 + ((Math.sin(index * 2.3) + 1) / 2) * 0.5;
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    const tokens = (millions: number) =>
      Math.round(millions * weekendFactor * jitter * 1_000_000);
    days.push({
      date: iso,
      values: {
        review: tokens(30 * 0.94),
        interview: tokens(16 * 1.08),
        testcase: tokens(9 * 0.7),
        practice: tokens(5 * 0.5),
      },
    });
  }
  return days;
}

const REVIEWED = new Set<string>();

// The second one is the anomaly warning that DEC-2026-0831-ai-usage-anomaly-alert confirms is a SOFT
// admin-facing warning, never an auto-lock.
const ALERTS: UsageAlert[] = [
  { kind: "budget" },
  {
    kind: "anomaly",
    alertId: "mock-alert-1",
    displayName: "nguyenvana",
    callCount: 214,
    ratio: 7,
  },
];

const user = (
  rank: number,
  name: string,
  tokens: number,
  calls: number,
  costUsd: number,
): TopUser => ({ userId: `mock-user-${rank}`, name, tokens, calls, costUsd });

// dc.html:499-504.
const TOP_USERS: TopUser[] = [
  user(1, "Nguyễn Văn An", 1_840_000, 214, 8.7),
  user(2, "Trần Thị Bích", 1_210_000, 156, 5.8),
  user(3, "Lê Hoàng Nam", 970_000, 131, 4.6),
  user(4, "Phạm Minh Đức", 860_000, 118, 4.1),
  user(5, "Vũ Thu Hà", 740_000, 102, 3.5),
];

const problem = (
  rank: number,
  title: string,
  levelCode: string,
  calls: number,
  tokens: number,
  avgTokensPerCall: number,
): TopProblem => ({
  problemId: `mock-problem-${rank}`,
  title,
  levelCode,
  levelName: null,
  tokens,
  calls,
  avgTokensPerCall,
});

// dc.html:513-519, relabelled as AI calls across the features that carry a problem id.
const TOP_PROBLEMS: TopProblem[] = [
  problem(1, "Đường đi ngắn nhất trên lưới", "HARD", 412, 2_100_000, 5100),
  problem(2, "Cây khung nhỏ nhất", "HARD", 298, 1_520_000, 5100),
  problem(3, "Chuỗi con chung dài nhất", "MEDIUM", 506, 1_440_000, 2850),
  problem(4, "Dãy con tăng dài nhất", "MEDIUM", 618, 1_310_000, 2120),
  problem(5, "Hai con trỏ trên mảng sắp xếp", "EASY", 742, 890_000, 1200),
];

export const mockDailyUsage = (range: UsageRange): DailyUsage[] =>
  buildDays({ "7d": 7, "14d": 14, "30d": 30 }[range]);

export const mockUsageAlerts = (): UsageAlert[] =>
  ALERTS.filter(
    (alert) => alert.kind === "budget" || !REVIEWED.has(alert.alertId),
  );

export function mockMarkAlertReviewed(alertId: string): void {
  REVIEWED.add(alertId);
}

/** Test hook: the reviewed set lives at module level. */
export const resetMockUsageAlerts = () => REVIEWED.clear();

// Without any price row the cost column shows "-" (BD ADM0302 Q2).
export const mockTopUsers = (): TopUser[] =>
  TOP_USERS.map((row) => ({
    ...row,
    costUsd: mockHasModelPrices() ? row.costUsd : null,
  }));

export const mockTopProblems = (): TopProblem[] =>
  TOP_PROBLEMS.map((row) => ({ ...row }));
