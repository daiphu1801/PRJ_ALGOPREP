// PROTOTYPE mock — no backend endpoint exists yet.
// Values from 09-layoutBase/Admin - Token AI.dc.html:450-525, with the cut "Gợi ý" feature removed
// (DEC-2026-0831-remove-tiered-hints-ai-config): the chart drops that series and the top-problems
// column stops counting "lượt gợi ý".
import type {
  AiUsagePage,
  AiUsageStat,
  DailyUsage,
  FeatureShare,
  TopProblem,
  TopUser,
  UsageAlert,
  UsageRange,
} from "../../model/types";

// dc.html:450-455.
const STATS: AiUsageStat[] = [
  { key: "monthTokens", value: "38,4 tr", delta: "+12%", deltaColorVar: "--color-admin-warn", meta: "Tính đến 22/08, 15:40" },
  { key: "dailyAverage", value: "1,74 tr", delta: "−4%", deltaColorVar: "--color-success", meta: "Thấp hơn tuần trước" },
  { key: "estimatedCost", value: "182 USD", delta: "+9%", deltaColorVar: "--color-admin-warn", meta: "Hạn mức 240 USD mỗi tháng" },
  { key: "calls", value: "9.418", delta: "+6%", deltaColorVar: "--color-admin-warn", meta: "2.140 người học hoạt động" },
];

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
    days.push({
      label: `${date.getDate()}/${date.getMonth() + 1}`,
      values: {
        review: Number((30 * weekendFactor * jitter * 0.94).toFixed(1)),
        interview: Number((16 * weekendFactor * jitter * 1.08).toFixed(1)),
        testcase: Number((9 * weekendFactor * jitter * 0.7).toFixed(1)),
      },
    });
  }
  return days;
}

// dc.html:482-486.
const FEATURE_SHARES: FeatureShare[] = [
  { feature: "review", label: "21,9 tr", percent: 57 },
  { feature: "interview", label: "12,3 tr", percent: 32 },
  { feature: "testcase", label: "4,2 tr", percent: 11 },
];

// dc.html:488-491. The second one is the anomaly warning that
// DEC-2026-0831-ai-usage-anomaly-alert confirms is a SOFT admin-facing warning, never an auto-lock.
const ALERTS: UsageAlert[] = [
  { key: "budgetRunningOut", tone: "warn" },
  { key: "anomalousAccount", tone: "negative" },
  { key: "testcasePromptCost", tone: "warn" },
];

// dc.html:499-504.
const TOP_USERS: TopUser[] = [
  { rank: 1, name: "Nguyễn Văn An", meta: "nguyenvana · K21", tokens: "1,84 tr", calls: 214, cost: "8,7 USD" },
  { rank: 2, name: "Trần Thị Bích", meta: "tranbich · K22", tokens: "1,21 tr", calls: 156, cost: "5,8 USD" },
  { rank: 3, name: "Lê Hoàng Nam", meta: "lhnam · K21", tokens: "0,97 tr", calls: 131, cost: "4,6 USD" },
  { rank: 4, name: "Phạm Minh Đức", meta: "pmduc · K23", tokens: "0,86 tr", calls: 118, cost: "4,1 USD" },
  { rank: 5, name: "Vũ Thu Hà", meta: "vuthuha · K22", tokens: "0,74 tr", calls: 102, cost: "3,5 USD" },
  { rank: 6, name: "Đỗ Quốc Bảo", meta: "dqbao · K23", tokens: "0,68 tr", calls: 94, cost: "3,2 USD" },
];

// dc.html:513-519. The mockup's meta column read "412 lượt gợi ý" etc.; the hint feature is gone,
// so the same counts are relabelled as AI calls across the three features still in scope.
const TOP_PROBLEMS: TopProblem[] = [
  { rank: 1, name: "Đường đi ngắn nhất trên lưới", difficulty: "hard", calls: 412, tokens: "2,10 tr", averagePerCall: "5.100" },
  { rank: 2, name: "Cây khung nhỏ nhất", difficulty: "hard", calls: 298, tokens: "1,52 tr", averagePerCall: "5.100" },
  { rank: 3, name: "Chuỗi con chung dài nhất", difficulty: "medium", calls: 506, tokens: "1,44 tr", averagePerCall: "2.850" },
  { rank: 4, name: "Dãy con tăng dài nhất", difficulty: "medium", calls: 618, tokens: "1,31 tr", averagePerCall: "2.120" },
  { rank: 5, name: "Hai con trỏ trên mảng sắp xếp", difficulty: "easy", calls: 742, tokens: "0,89 tr", averagePerCall: "1.200" },
  { rank: 6, name: "Đếm số đảo ngược", difficulty: "hard", calls: 184, tokens: "0,81 tr", averagePerCall: "4.400" },
];

export function fetchAiUsagePage(): AiUsagePage {
  const daily: Record<UsageRange, DailyUsage[]> = {
    "7d": buildDays(7),
    "14d": buildDays(14),
    "30d": buildDays(30),
  };

  return {
    stats: STATS.map((stat) => ({ ...stat })),
    daily,
    featureShares: FEATURE_SHARES.map((share) => ({ ...share })),
    alerts: ALERTS.map((alert) => ({ ...alert })),
    topUsers: TOP_USERS.map((user) => ({ ...user })),
    topProblems: TOP_PROBLEMS.map((problem) => ({ ...problem })),
    // dc.html:527-528.
    budget: {
      usedLabel: "38,4 tr",
      capLabel: "52,0 tr",
      leftLabel: "13,6 tr",
      percent: 74,
      runOutLabel: "28/08",
    },
  };
}
