// Shapes of the AI budget slice (F5 #18 to #26). The server DTOs are mapped to these in api/real-*.ts.
import type { ManagedItem } from "@/shared/lib/managed-list-store";

/** The four F5 features that spend tokens (feature_code SOLUTION_REVIEW, MOCK_INTERVIEW, GENERATE_TESTCASE, PRACTICE_FEEDBACK). */
export type AiFeature = "review" | "interview" | "testcase" | "practice";

export const AI_FEATURES: AiFeature[] = [
  "review",
  "interview",
  "testcase",
  "practice",
];

/** Monthly token cap, usage and lock (#18 to #20). `limitTokens` 0 means "not configured". */
export type TokenBudget = {
  limitTokens: number;
  usedTokens: number;
  locked: boolean;
  /** The bar changes colour once usage reaches this percent of the cap (same value as `RateLimits.warnPercent`). */
  warnPercent: number;
  /** ISO date (yyyy-MM-dd), or null when locked, uncapped or idle. */
  projectedRunOutDate: string | null;
  /** Tokens per feature this period; every feature present, 0 when unused. */
  featureTokens: Record<AiFeature, number>;
};

/** Per-user AI limits and the budget warning threshold (#21, #22). */
export type RateLimits = {
  reviewPerHour: number;
  interviewPerHour: number;
  cooldownSeconds: number;
  warnPercent: number;
};

/** The body of #22: one or more fields. */
export type RateLimitsPatch = Partial<RateLimits>;

/** A row of the price table (#23 to #26): `key` is the price id, `label` the model name. */
export type ModelPrice = ManagedItem & {
  /** USD per one million tokens. */
  usdPerMillionTokens: number;
};

/** Largest cap the server accepts (BudgetRequests.MAX_LIMIT_TOKENS). */
export const MAX_LIMIT_TOKENS = 9_000_000_000_000;
