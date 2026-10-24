/**
 * AI token consumption (F5-21, F5-25). Four AI features remain in scope:
 * solution review (F5.1), mock interview (F5.2), testcase generation (F2-14) and practice feedback (F6-08).
 *
 * "Gợi ý theo bậc" (tiered in-progress hints) was cut entirely by
 * DEC-2026-0831-remove-tiered-hints-ai-config, so it is absent here even though the static mockup
 * still charts it as a fourth series and labels the top-problems column "lượt gợi ý".
 *
 * `testcase` maps to `feature_code = GENERATE_TESTCASE` and its tokens are keyed by the problem id
 * (DEC-2026-1010-ai-generate-testcase-feature-code, 02-bd/database/F5_ai-review.md sections 1.1 and 1.6).
 * `practice` maps to `feature_code = PRACTICE_FEEDBACK`: the AI feedback of the interview-bank practice mode
 * (F6-08), counted in the shared budget by DEC-2026-1011-ai-practice-feedback-in-shared-budget.
 */
export type AiFeature = "review" | "interview" | "testcase" | "practice";

export const AI_FEATURES: AiFeature[] = [
  "review",
  "interview",
  "testcase",
  "practice",
];

export type UsageRange = "7d" | "14d" | "30d";

/** One day on the stacked chart: tokens per feature, in millions. */
export type DailyUsage = {
  label: string;
  values: Record<AiFeature, number>;
};

export type FeatureShare = {
  feature: AiFeature;
  /** Pre-formatted total, e.g. "21,9 tr". */
  label: string;
  percent: number;
};

export type UsageAlert = {
  key: string;
  tone: "warn" | "negative";
};

export type TopUser = {
  rank: number;
  name: string;
  /** Username and cohort, e.g. "nguyenvana · K21". */
  meta: string;
  tokens: string;
  calls: number;
  cost: string;
};

export type TopProblem = {
  rank: number;
  name: string;
  difficulty: string;
  /** Call count across the in-scope AI features. */
  calls: number;
  tokens: string;
  averagePerCall: string;
};

export type AiUsagePage = {
  daily: Record<UsageRange, DailyUsage[]>;
  featureShares: FeatureShare[];
  alerts: UsageAlert[];
  topUsers: TopUser[];
  topProblems: TopProblem[];
  budget: {
    runOutLabel: string;
  };
};
