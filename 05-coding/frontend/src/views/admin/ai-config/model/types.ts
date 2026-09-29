/**
 * AI assistant configuration (F5-23): one versioned prompt per AI feature, rubric weights, rate
 * limits and answer guards.
 *
 * Three features, matching entities/ai-usage: solution review, mock interview, testcase generation.
 * "Gợi ý theo bậc" was cut by DEC-2026-0831-remove-tiered-hints-ai-config and this mockup was
 * already updated for it — unlike the AI-usage one.
 *
 * Testcase generation carries the same ownership gap noted in entities/ai-usage: there is no
 * GENERATE_TESTCASE in `prompt_templates.feature_code` (level B2 in
 * 07-review/bd_screens_admin_open_questions_260913.md).
 */
export type AiFeatureKey = "review" | "interview" | "testcase";

export type PromptStatus = "live" | "draft" | "abTest";

export type PromptConfig = {
  feature: AiFeatureKey;
  version: string;
  status: PromptStatus;
  model: string;
  /** Sampling temperature as shown, e.g. "0,2". */
  temperature: string;
  maxTokens: string;
  updatedAt: string;
};

export type RubricKey = "correctness" | "complexity" | "codeQuality" | "edgeCases" | "explanation";

export const RUBRIC_KEYS: RubricKey[] = [
  "correctness",
  "complexity",
  "codeQuality",
  "edgeCases",
  "explanation",
];

export type RateLimitKey = "reviewPerDay" | "interviewPerWeek" | "cooldown";

export const RATE_LIMIT_KEYS: RateLimitKey[] = [
  "reviewPerDay",
  "interviewPerWeek",
  "cooldown",
];

/**
 * Answer guards. These are the prompt-level rules the assistant must follow — they are part of the
 * SYSTEM instruction, never mixed with learner code, per the prompt-injection rule in CLAUDE.md.
 */
export type GuardKey = "noFullSolution" | "citeLines" | "answerInVietnamese";

export const GUARD_KEYS: GuardKey[] = ["noFullSolution", "citeLines", "answerInVietnamese"];

export type PromptVersionEntry = {
  id: string;
  feature: AiFeatureKey;
  version: string;
  publishedAt: string;
  author: string;
};

export type AiConfigPage = {
  prompts: PromptConfig[];
  rubricWeights: Record<RubricKey, number>;
  rateLimits: Record<RateLimitKey, string>;
  guards: Record<GuardKey, boolean>;
  versionHistory: PromptVersionEntry[];
  lastComparison: { date: string; averageDelta: string };
};

export function totalWeight(weights: Record<RubricKey, number>): number {
  return RUBRIC_KEYS.reduce((sum, key) => sum + weights[key], 0);
}
