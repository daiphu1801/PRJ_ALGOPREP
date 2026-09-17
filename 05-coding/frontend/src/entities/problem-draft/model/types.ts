/**
 * Problem being authored (F2-01..F2-09). Shared A2/A3 screen, mounted at /instructor and /admin
 * (DEC-2026-0825-shared-content-authoring-screens).
 *
 * NO per-testcase weights and no partial-score block. PROTOTYPE_DEBT 2.6 removed both; the
 * remaining partial score (F4-13) is an automatic pass-ratio computed by judge-orchestration, not
 * something the author declares here.
 */
export type TestcaseVisibility = "public" | "hidden";

export type Testcase = {
  id: string;
  input: string;
  expected: string;
  visibility: TestcaseVisibility;
};

export type WorkedExample = {
  id: string;
  input: string;
  output: string;
  explanation: string;
};

export type ProblemLimits = {
  timeLimitMs: number;
  memoryLimitMb: number;
  outputLimitKb: number;
  stackLimitMb: number;
};

/**
 * Per-problem instructions appended to the AI system prompt. The mockup has a third flag,
 * "Cho phép AI mở gợi ý ẩn", which belongs to the tiered-hint feature cut by
 * DEC-2026-0831-remove-tiered-hints-ai-config — it is absent here and raised in the phase report.
 */
export type AiGuardKey = "noFullCode" | "socraticOnly";

export const AI_GUARD_KEYS: AiGuardKey[] = ["noFullCode", "socraticOnly"];

export type ProblemDraft = {
  title: string;
  /** Markdown body. */
  body: string;
  constraints: string;
  topic: string;
  difficulty: "easy" | "medium" | "hard";
  status: "draft" | "published";
  limits: ProblemLimits;
  solutionLanguage: string;
  solution: string;
  examples: WorkedExample[];
  testcases: Testcase[];
  aiBrief: string;
  aiGuards: Record<AiGuardKey, boolean>;
};
