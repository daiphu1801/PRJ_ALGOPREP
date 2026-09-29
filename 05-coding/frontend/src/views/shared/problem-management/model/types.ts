/**
 * Problem as the management list sees it. Shared screen: A2 and A3 both reach it, mounted under
 * both /instructor and /admin (DEC-2026-0825-shared-content-authoring-screens).
 *
 * TWO lifecycle states only — draft and published. DEC-2026-0830-problem-lifecycle-two-states drops
 * the mockup's third "Đã ẩn" state and adds code F2-15. The mockup still carries it on one row and
 * in a bulk action labelled "Xuất bản / ẩn".
 */
export type ProblemStatus = "draft" | "published";

export type Difficulty = "easy" | "medium" | "hard";

export type AdminProblem = {
  /** Display code, e.g. "#1143". Doubles as the row key. */
  code: string;
  title: string;
  topic: string;
  difficulty: Difficulty;
  status: ProblemStatus;
  submissionCount: number;
  /** Accepted rate, 0-100. */
  acceptedRate: number;
  testcaseCount: number;
  /** Relative time of the last edit, e.g. "3 ngày trước". */
  editedLabel: string;
};

export type ProblemStat = {
  key: string;
  value: string;
  delta: string;
  deltaColorVar: string;
};

export type AdminProblemPage = {
  stats: ProblemStat[];
  problems: AdminProblem[];
  totalProblems: number;
  publishedCount: number;
};

export type ProblemSortKey =
  | "title"
  | "topic"
  | "difficulty"
  | "status"
  | "submissions"
  | "accepted"
  | "edited";
