// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Shared submission types for USR0201 (submission_result) and USR0202 (my_submissions) — same
// Bounded Context (judge-orchestration), same underlying table `judge.submissions`
// (02-bd/screens/users/USR0201_submission_result.md Sheet 4.3), so one entity instead of two.

/** Verdict of the whole submission. Matches `judge.submissions.status`. */
export type SubmissionStatus =
  | "PENDING"
  | "COMPILING"
  | "JUDGING"
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "RUNTIME_ERROR"
  | "COMPILE_ERROR";

export type SubmissionLanguage = "PYTHON" | "CPP" | "JAVA";

/** F3-13. */
export type SubmissionMode = "FUNCTION_WRAPPER" | "STANDARD_IO";

export type Difficulty = "EASY" | "MEDIUM" | "HARD";

/** `problem.testcases.visibility` — never confuse with a boolean `is_sample`, per BD V0.2 fix. */
export type TestcaseVisibility = "SAMPLE" | "HIDDEN";

/** Per-testcase verdict, distinct from the submission-level SubmissionStatus. */
export type TestcaseVerdict = "AC" | "WA" | "TLE" | "MLE" | "RE";

export type SubmissionTestcaseResult = {
  testcaseId: string;
  order: number;
  visibility: TestcaseVisibility;
  verdict: TestcaseVerdict;
  runtimeMs: number;
  memoryKb: number;
  // Sample-only fields (F2-08: hidden testcase input/output must never reach the client).
  sampleInput?: string;
  sampleExpectedOutput?: string;
  sampleActualOutput?: string;
};

export type SubmissionDetail = {
  id: string;
  problemId: string;
  problemSlug: string;
  problemTitle: string;
  difficulty: Difficulty;
  language: SubmissionLanguage;
  submissionMode: SubmissionMode;
  status: SubmissionStatus;
  /** Null until judging finishes ("PENDING"/"COMPILING"/"JUDGING" still run). */
  passedCount: number | null;
  totalCount: number | null;
  runtimeMs: number | null;
  memoryKb: number | null;
  /** F4-12. Null unless status === "ACCEPTED". */
  beatsPercent: number | null;
  submittedAt: string;
  sourceCode: string;
  /** Only set when status === "COMPILE_ERROR" (Khu vực E NO 4). */
  compileErrorMessage: string | null;
  /** Empty while status === "COMPILE_ERROR" — the table is hidden entirely, not just empty. */
  testcases: SubmissionTestcaseResult[];
};

export type SubmissionListItem = {
  id: string;
  problemId: string;
  problemSlug: string;
  problemCode: string;
  problemTitle: string;
  difficulty: Difficulty;
  language: SubmissionLanguage;
  status: SubmissionStatus;
  passedCount: number | null;
  totalCount: number | null;
  runtimeMs: number | null;
  submittedAt: string;
};

/**
 * Raw counters only — no `acceptedRate` column exists (BD V0.2 fix note). The screen computes every
 * rate from these at render time.
 */
export type SubmissionStats = {
  totalSubmissions: number;
  acceptedCount: number;
  firstTryAcceptedCount: number;
  topLanguage: SubmissionLanguage | null;
  topLanguageCount: number;
};

export type VerdictFilter = SubmissionStatus | "ALL";
export type LanguageFilter = SubmissionLanguage | "ALL";

export type ListMySubmissionsParams = {
  query?: string;
  verdict?: VerdictFilter;
  language?: LanguageFilter;
  page?: number;
  pageSize?: number;
};

export type SubmissionListPage = {
  items: SubmissionListItem[];
  currentPage: number;
  pageSize: number;
  totalItems: number;
};
