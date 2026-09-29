/**
 * Problem being authored (F2-01..F2-09). Shared A2/A3 screen, mounted at /instructor and /admin
 * (DEC-2026-0825-shared-content-authoring-screens).
 *
 * NO per-testcase weights and no partial-score block. PROTOTYPE_DEBT 2.6 removed both; the
 * remaining partial score (F4-13) is an automatic pass-ratio computed by judge-orchestration, not
 * something the author declares here.
 */
export type TestcaseVisibility = "public" | "hidden";

/**
 * Where a testcase came from, and whether a human has signed off on it (F2-14, RD amended
 * 2026-09-28). AI output lands as an unapproved draft: only approved rows count toward the
 * publish checklist, because a testcase nobody has looked at must not decide a student's grade.
 */
export type TestcaseOrigin = "manual" | "ai";

/** Case class the generator script claims a row covers — drives the coverage matrix (F2-14). */
export type TestcaseCategory =
  | "typical"
  | "boundaryLow"
  | "boundaryHigh"
  | "degenerate"
  | "duplicate"
  | "sorted"
  | "extreme";

export const TESTCASE_CATEGORIES: TestcaseCategory[] = [
  "typical",
  "boundaryLow",
  "boundaryHigh",
  "degenerate",
  "duplicate",
  "sorted",
  "extreme",
];

export type Testcase = {
  id: string;
  input: string;
  expected: string;
  visibility: TestcaseVisibility;
  origin: TestcaseOrigin;
  approved: boolean;
  category?: TestcaseCategory;
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
  /**
   * Result of the last "Chạy với đáp án mẫu" run (F2-18). Gates two things: the generate button
   * (F2-14 needs >= 2 author-written Sample rows that the reference solution passes) and publish.
   */
  solutionCheck: { ran: boolean; passed: number; total: number };
  aiBrief: string;
  aiGuards: Record<AiGuardKey, boolean>;
};
