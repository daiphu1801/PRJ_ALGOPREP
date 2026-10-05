// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Shared student-facing `problem` entity — used by problem-list, problem-detail and
// saved-problems (02-bd/screens/users/USR0101_problem_list.md:282-283 explicitly reuses one
// `entities/problem` across the three; DO NOT reuse `entities/admin-problem`, which is the
// instructor/admin-side shape from a different screen family).

/** Stable key of an admin-managed problem difficulty level (see ./level-store). */
export type Difficulty = string;

/**
 * MOCK MERGE NOTE: in the real system, `solveState` is NOT returned by `problem-bank`.
 * `problem-bank` only classifies a problem (topic/difficulty/submission model); per-user solve
 * state comes from `identity.user_problem_best_score`
 * (02-bd/screens/users/USR0101_problem_list.md Sheet 4.3, row 6 — "Bốn bảng cuối nằm ở schema của
 * module khác... `problem-bank` gọi qua cổng ra sang `identity`"). See also
 * 07-review/bd_open_questions_with_solutions_260924.md Q2/Q3. This prototype has no real API
 * boundary yet, so the mock merges both sources into one flat field — flagged here so graduation
 * to DD knows to split it back into two calls (or a BFF join).
 */
export type SolveState = "solved" | "attempted" | "todo";

/**
 * `function_wrapper_supported` from `problem.problems`
 * (02-bd/database/problem-bank.md:20) — "both" is the default post
 * `DEC-2026-0824-dual-submission-model-per-problem`; "stdioOnly" is the harness.md F3-13 type-schema
 * exception (R2), where Function Wrapper is hidden and only Standard I/O is offered.
 */
export type SubmissionModel = "both" | "stdioOnly";

export type ProblemListItem = {
  id: string;
  code: string;
  title: string;
  solveState: SolveState;
  submissionModel: SubmissionModel;
  hasSolutionReview: boolean;
  topics: string[];
  difficulty: Difficulty;
  /** 0-100, or null when `problem_stats` has no row yet (displayed as "-"). */
  acRate: number | null;
};

export type RatioStat = { solved: number; total: number };

export type ProblemCatalogSummary = {
  solvedTotal: RatioStat;
  /** Solved / total per level key (admin-managed list); a level missing here has no problems yet. */
  solvedByLevel: Readonly<Record<string, RatioStat>>;
};

export type TopicProgress = { id: string; name: string } & RatioStat;

export type SubmissionLanguage = "java" | "cpp" | "python";

export type InProgressProblem = {
  problemId: string;
  title: string;
  language: SubmissionLanguage;
  /** Already display-mapped, e.g. "Sai kết quả 8/12" — mapping from `submissions.status` is BD/DD's job. */
  lastVerdictLabel: string;
};

export type ClassAssignmentItem = {
  problemId: string;
  title: string;
  solveState: SolveState;
  /** e.g. "3 / 5 bài · giao 24/08" — see BD Q9, hạn nộp chưa có cột DB thật. */
  metaLabel: string;
};

export type ClassAssignmentGroup = {
  className: string;
  items: ClassAssignmentItem[];
};

export type ProblemListPage = {
  summary: ProblemCatalogSummary;
  topics: TopicProgress[];
  items: ProblemListItem[];
  inProgress: InProgressProblem[];
  classAssignments: ClassAssignmentGroup[];
};

export type ProblemListFilter = {
  query: string;
  status: SolveState | "all";
  difficulty: Difficulty | "all";
  topicId: string | "all";
  sort: "difficulty" | "acRate";
  direction: "asc" | "desc";
  page: number;
};

// --- Bài đã lưu (USR0103) ---------------------------------------------------------------------

export type SavedProblem = {
  id: string;
  code: string;
  title: string;
  difficulty: Difficulty;
  topics: string[];
  solveState: SolveState;
  /** Private note, absolute per (user_id, problem_id) — F2-13. Never shared, not even with A2/A3. */
  note: string;
  savedAtLabel: string;
};

// --- Chi tiết bài tập / Workspace (USR0102) ---------------------------------------------------

export type WrapperMode = "stdio" | "function";
export type InputMethod = "type" | "upload";

export type ProblemExample = {
  input: string;
  output: string;
  explanation?: string;
};

export type StarterCodeKey = `${SubmissionLanguage}:${WrapperMode}`;

export type SubmissionHistoryItem = {
  id: string;
  verdictLabel: string;
  language: SubmissionLanguage;
  runtimeMs: number | null;
  submittedAtLabel: string;
};

export type SolutionReviewQuickInsight = {
  timeComplexity: string;
  spaceComplexity: string;
  notes: string[];
};

export type ProblemDetail = {
  id: string;
  code: string;
  title: string;
  difficulty: Difficulty;
  topics: string[];
  submissionModel: SubmissionModel;
  statementMd: string;
  examples: ProblemExample[];
  constraints: string[];
  timeLimitMs: number;
  memoryLimitMb: number;
  acRate: number | null;
  starterCode: Record<StarterCodeKey, string>;
  sampleTestcases: { input: string; expectedOutput: string }[];
  mySubmissions: SubmissionHistoryItem[];
  solutionReview: SolutionReviewQuickInsight | null;
};

export type TestcaseVerdict = "pending" | "running" | "passed" | "failed";

export type OverallVerdict =
  | "accepted"
  | "wrongAnswer"
  | "runtimeError"
  | "timeLimitExceeded"
  | "running";

export type SubmissionRunResult = {
  submissionId: string;
  overallVerdict: OverallVerdict;
  passedCount: number;
  totalCount: number;
  testcases: { order: number; verdict: TestcaseVerdict }[];
};

export type SampleRunResult = {
  cases: { input: string; expectedOutput: string; actualOutput: string; passed: boolean }[];
};
