/**
 * AI token consumption (F5-21, F5-25). Three AI features remain in scope:
 * solution review (F5.1), mock interview (F5.2) and testcase generation (F2-14).
 *
 * "Gợi ý theo bậc" (tiered in-progress hints) was cut entirely by
 * DEC-2026-0831-remove-tiered-hints-ai-config, so it is absent here even though the static mockup
 * still charts it as a fourth series and labels the top-problems column "lượt gợi ý".
 *
 * `testcase` carries a known ownership gap: `prompt_templates.feature_code` in
 * 02-bd/database/ai-review.md section 1.1 only enumerates SOLUTION_REVIEW and MOCK_INTERVIEW, with
 * no GENERATE_TESTCASE. Level B2 in 07-review/bd_screens_admin_open_questions_260913.md — it needs
 * an ai-review / problem-bank reconciliation before DD.
 */
export type AiFeature = "review" | "interview" | "testcase";

export const AI_FEATURES: AiFeature[] = ["review", "interview", "testcase"];

export type UsageRange = "7d" | "14d" | "30d";

export type AiUsageStat = {
  key: string;
  value: string;
  delta: string;
  deltaColorVar: string;
  meta: string;
};

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
  difficulty: "easy" | "medium" | "hard";
  /** Call count across the in-scope AI features. */
  calls: number;
  tokens: string;
  averagePerCall: string;
};

export type AiUsagePage = {
  stats: AiUsageStat[];
  daily: Record<UsageRange, DailyUsage[]>;
  featureShares: FeatureShare[];
  alerts: UsageAlert[];
  topUsers: TopUser[];
  topProblems: TopProblem[];
  budget: {
    usedLabel: string;
    capLabel: string;
    leftLabel: string;
    percent: number;
    runOutLabel: string;
  };
};
