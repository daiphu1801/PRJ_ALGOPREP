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
  difficulty: string;
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
  spec: ProblemSpec;
  aiBrief: string;
  aiGuards: Record<AiGuardKey, boolean>;
};

/** What the editor, the read-only page and the preview all load: the problem plus its last save time. */
export type ProblemDraftRecord = {
  draft: ProblemDraft;
  /** ISO time of the last save, or null when the problem was never saved. */
  savedAt: string | null;
  /**
   * F2-14 quota: how many times the author already generated testcases for this problem, and the
   * cap. The counter lives on the server (the client cannot be trusted with it); the cap is an
   * ADMIN-configurable setting (DEC-2026-1001-admin-configurable-settings), default 2.
   */
  aiGeneration: { used: number; limit: number };
};

/** Why a generated input was thrown away instead of becoming a draft testcase. */
export type DroppedReason = "invalidConstraint" | "referenceFailed" | "timeout";

/** `GenerateTestcasesWithAi` request (BD SHR0202 EVT-16). Never carries the reference solution. */
export type GenerateTestcasesInput = {
  problemId?: string;
  /** Statement Markdown, sent to the AI as data. */
  body: string;
  constraints: string;
  /** The author-written Sample rows that anchor the input format. */
  samples: Pick<Testcase, "input" | "expected">[];
};

/** `GenerateTestcasesWithAi` response: new unapproved draft rows plus what was discarded. */
export type GenerateTestcasesResult = {
  testcases: Testcase[];
  dropped: { reason: DroppedReason; count: number }[];
  /** The largest case failed or timed out on the reference solution: F2-10 limit may be wrong. */
  largestCaseWarning: boolean;
};

// ---------------------------------------------------------------------------------------------
// Problem spec: the "Đặc tả" tab (F2-03, F2-04; BD SHR0202 Sheet 5 Khu vực F). It feeds the harness
// (function-wrapper model) and Standard I/O. Simple prototype: one level of container type, no nested
// type picker (BD SHR0202 Q10).
// ---------------------------------------------------------------------------------------------

/** Element-level kinds of the harness type schema (02-bd/architecture/harness.md section 4.2). */
export type ScalarKind =
  "INT" | "LONG" | "DOUBLE" | "BOOLEAN" | "CHAR" | "STRING";

export const SCALAR_KINDS: ScalarKind[] = [
  "INT",
  "LONG",
  "DOUBLE",
  "BOOLEAN",
  "CHAR",
  "STRING",
];

/**
 * `OTHER` is a prototype affordance, not a schema kind: it stands for "a type the schema cannot
 * express" so the F3-13 warning (problem falls back to Standard I/O only) can be shown. The real
 * server derives that flag from the saved JSON.
 */
export type TypeKind =
  ScalarKind | "ARRAY" | "LIST" | "LINKED_LIST" | "BINARY_TREE" | "OTHER";

export const TYPE_KINDS: TypeKind[] = [
  ...SCALAR_KINDS,
  "ARRAY",
  "LIST",
  "LINKED_LIST",
  "BINARY_TREE",
  "OTHER",
];

/** Kinds that carry an element type (`of`); ARRAY also carries a dimension count. */
export const CONTAINER_KINDS: TypeKind[] = [
  "ARRAY",
  "LIST",
  "LINKED_LIST",
  "BINARY_TREE",
];

export type SpecType = {
  kind: TypeKind;
  /** Element type of a container; only LIST may hold a container (nesting, up to MAX_TYPE_NESTING levels). */
  of?: SpecType;
  /** ARRAY only: 1 to 3. */
  dimensions?: number;
};

export type SpecParameter = { id: string; name: string; type: SpecType };

export type SpecLanguage = "java" | "cpp" | "python";

export const SPEC_LANGUAGES: SpecLanguage[] = ["java", "cpp", "python"];

/**
 * ONE signature per problem (owner 2026-10-03, replaces three per-language rows): the harness maps
 * the types to each language itself, so only the spelling of the function name differs.
 */
export type FunctionSignature = {
  /** Shared name, typed in either snake_case or camelCase; each language's spelling is derived from it. */
  functionName: string;
  returnType: SpecType;
  /** Order matters; names appear in the starter code and in how a sample testcase is displayed. */
  parameters: SpecParameter[];
  /** Optional spelling that replaces the derived name for one language. */
  nameOverrides: Partial<Record<SpecLanguage, string>>;
};

export type MatchingStrategy =
  "EXACT" | "TRIMMED" | "EPSILON" | "UNORDERED_SET";

export const MATCHING_STRATEGIES: MatchingStrategy[] = [
  "EXACT",
  "TRIMMED",
  "EPSILON",
  "UNORDERED_SET",
];

export type ProblemSpec = {
  /** The one shared function signature. */
  signature: FunctionSignature;
  stdinFormat: string;
  stdoutFormat: string;
  matchingStrategy: MatchingStrategy;
  /** Tolerance, kept as text so a half-typed number does not jump around; used when EPSILON. */
  epsilon: string;
};

/** How many container levels one type may nest, e.g. `List<List<List<int>>>` is 3 (BD SHR0202 Q10). */
export const MAX_TYPE_NESTING = 3;
