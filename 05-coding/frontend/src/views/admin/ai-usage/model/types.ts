/**
 * AI token consumption (F5-21, F5-25). Four AI features remain in scope:
 * solution review (F5.1), mock interview (F5.2), testcase generation (F2-14) and practice feedback (F6-08).
 *
 * "Gợi ý theo bậc" (tiered in-progress hints) was cut entirely by
 * DEC-2026-0831-remove-tiered-hints-ai-config, so it is absent here.
 *
 * `testcase` maps to `feature_code = GENERATE_TESTCASE`, `practice` to `PRACTICE_FEEDBACK`
 * (DEC-2026-1010-ai-generate-testcase-feature-code, DEC-2026-1011-ai-practice-feedback-in-shared-budget).
 */
import type { AiFeature } from "@/entities/ai-budget";

export { AI_FEATURES, type AiFeature } from "@/entities/ai-budget";

export type UsageRange = "7d" | "14d" | "30d";

/** One UTC+7 day on the stacked chart: raw tokens per feature. */
export type DailyUsage = {
  /** ISO date, yyyy-MM-dd. */
  date: string;
  values: Record<AiFeature, number>;
};

/** The budget alert is derived by the server (no id, nothing to mark); an anomaly alert can be marked reviewed. */
export type UsageAlert =
  | { kind: "budget" }
  | {
      kind: "anomaly";
      alertId: string;
      /** Null when the identity module could not name the account. */
      displayName: string | null;
      callCount: number;
      /** Calls in the 24 h window over the account's own average. */
      ratio: number;
    };

export type TopUser = {
  userId: string;
  /** Null when the identity module is unavailable. */
  name: string | null;
  tokens: number;
  calls: number;
  /** Null while any model the user spent on has no price: shown as "-", never estimated. */
  costUsd: number | null;
};

export type TopProblem = {
  problemId: string;
  /** Null when the problem-bank module is unavailable. */
  title: string | null;
  levelCode: string | null;
  levelName: string | null;
  tokens: number;
  calls: number;
  avgTokensPerCall: number;
};
