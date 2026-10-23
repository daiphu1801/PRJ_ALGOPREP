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

export type TopicProgress = { id: string; name: string } & RatioStat;

export type SubmissionLanguage = "java" | "cpp" | "python";

export type InProgressProblem = {
  problemId: string;
  title: string;
  language: SubmissionLanguage;
  /** Raw `submissions.status` plus the pass ratio; the view maps it to text through i18n (F4-13). */
  lastVerdict: OverallVerdict;
  passedCount: number;
  totalCount: number;
};

export type ClassAssignmentItem = {
  problemId: string;
  title: string;
  solveState: SolveState;
  /** Learner progress inside the class assignment; the view formats the label (BD Q9: no due date column). */
  solvedCount: number;
  totalCount: number;
  /** ISO timestamp the assignment was given. */
  assignedAt: string;
};

export type ClassAssignmentGroup = {
  className: string;
  items: ClassAssignmentItem[];
};

export type ProblemListPage = {
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
  /** Unpublished or soft-hidden after the learner saved it (USR0103 Q6): still listed, cannot be opened. */
  withdrawn?: boolean;
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
  verdict: OverallVerdict;
  passedCount: number;
  totalCount: number;
  language: SubmissionLanguage;
  runtimeMs: number | null;
  memoryKb: number | null;
  /** ISO timestamp; the view formats it (UTC+7). */
  submittedAt: string;
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
  /** Markdown incl. the `## Ràng buộc` and `## Câu hỏi mở rộng` sections (USR0102 Q5): no separate column. */
  statementMd: string;
  examples: ProblemExample[];
  /** Effective limits per language = base limit x language multiplier (BD Sheet 5 khu H NO 7). */
  limitsByLanguage: Record<
    SubmissionLanguage,
    { timeLimitMs: number; memoryLimitMb: number }
  >;
  acRate: number | null;
  /** Whether the current learner bookmarked this problem (`problem.bookmarks`). */
  saved: boolean;
  /** Only the count of hidden testcases is ever disclosed, never their content (BD Sheet 9 NO 3). */
  hiddenTestcaseCount: number;
  /** Mock only: every simulated Run Code / Submit passes (to review the all-passed UI). */
  mockAlwaysPass?: boolean;
  starterCode: Record<StarterCodeKey, string>;
  sampleTestcases: { input: string; expectedOutput: string }[];
  mySubmissions: SubmissionHistoryItem[];
  solutionReview: SolutionReviewQuickInsight | null;
};

export type TestcaseVerdict = "pending" | "running" | "passed" | "failed";

// Covers every `judge.submissions.status` (USR0104 Q5: one label per status, no raw keys).
export type OverallVerdict =
  | "accepted"
  | "wrongAnswer"
  | "runtimeError"
  | "timeLimitExceeded"
  | "running"
  | "pending"
  | "compiling"
  | "compileError"
  | "systemError";

export type SubmissionRunResult = {
  submissionId: string;
  overallVerdict: OverallVerdict;
  passedCount: number;
  totalCount: number;
  testcases: { order: number; verdict: TestcaseVerdict }[];
  /** Slowest testcase and largest memory among the testcases that ran (BD khu I NO 6). */
  runtimeMs: number;
  memoryKb: number;
};

export type SampleRunResult = {
  cases: {
    input: string;
    expectedOutput: string;
    actualOutput: string;
    passed: boolean;
    runtimeMs: number;
    memoryKb: number;
  }[];
};
