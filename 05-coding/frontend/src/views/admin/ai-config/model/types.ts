/**
 * AI quality configuration (F5-23): one versioned prompt per AI feature, its model and answer guards
 * (no rubric: DEC-2026-1014-drop-solution-review-rubric). Rate limits, the token cap and prices moved to admin_ai_limits on 2026-10-10
 * (DEC-2026-1010-split-ai-admin-screens).
 *
 * Three features, matching entities/ai-usage: solution review, mock interview, testcase generation.
 * "Gợi ý theo bậc" was cut by DEC-2026-0831-remove-tiered-hints-ai-config and this mockup was
 * already updated for it — unlike the AI-usage one.
 *
 * `testcase` maps to `feature_code = GENERATE_TESTCASE` (DEC-2026-1010-ai-generate-testcase-feature-code,
 * which closed level B2 in 07-review/bd_screens_admin_open_questions_260913.md).
 */
export type AiFeatureKey = "review" | "interview" | "testcase";

export const FEATURE_KEYS: AiFeatureKey[] = ["review", "interview", "testcase"];

// ACTIVE and DRAFT only: the DB enum has ARCHIVED too but an archived prompt is never listed here.
// "abTest" was dropped (BD ADM0301 V1.0): one ACTIVE prompt per feature, no A/B.
export type PromptStatus = "live" | "draft";

export type PromptConfig = {
  feature: AiFeatureKey;
  version: string;
  status: PromptStatus;
  /** One of AiConfigPage.modelOptions: the model is picked by ADMIN, the list is declared by the dev. */
  modelId: string;
  /** System instruction of the prompt (Lớp 1). Learner code and answers never get concatenated into it. */
  systemInstruction: string;
  maxTokens: string;
  updatedAt: string;
};

/**
 * Answer guards. These are the prompt-level rules the assistant must follow — they are part of the
 * SYSTEM instruction, never mixed with learner code, per the prompt-injection rule in CLAUDE.md.
 */
export type GuardKey = "noFullSolution" | "citeLines" | "answerInVietnamese";

export const GUARD_KEYS: GuardKey[] = [
  "noFullSolution",
  "citeLines",
  "answerInVietnamese",
];

/** Features that talk to the learner carry answer guards; testcase generation does not. */
export type GuardedFeature = "review" | "interview";

export const GUARDED_FEATURES: GuardedFeature[] = ["review", "interview"];

export type GuardSet = Record<GuardKey, boolean>;

export type PromptVersionEntry = {
  id: string;
  feature: AiFeatureKey;
  version: string;
  publishedAt: string;
  author: string;
  /** Instruction as it was in that version, copied into the draft by "Khôi phục". */
  systemInstruction: string;
};

export type AiConfigPage = {
  prompts: PromptConfig[];
  /** Models the dev has approved in the backend config; ADMIN picks from these, never types one. */
  modelOptions: string[];
  guards: Record<GuardedFeature, GuardSet>;
  versionHistory: PromptVersionEntry[];
};
