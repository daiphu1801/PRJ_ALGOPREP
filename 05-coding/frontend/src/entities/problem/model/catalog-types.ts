// Student catalogue as the real api answers it (03-dd/api/F2_problem-bank.md #1 to #4). Unlike the older
// `ProblemListPage` (one mock-only bundle that the dashboard and the workspace home still read), every
// figure that another module supplies can be unknown here, and unknown is never turned into a number.
import type { PageMeta } from "@/shared/api";
import type {
  Difficulty,
  InProgressProblem,
  SolveState,
  SubmissionModel,
} from "./types";

export type CatalogRow = {
  id: string;
  code: string;
  title: string;
  topics: string[];
  /** The level's code, the same key `useProblemLevels` lists. */
  difficulty: Difficulty;
  submissionModel: SubmissionModel;
  /** 0-100, or null while the problem has no statistics row (shown as "-"). */
  acRate: number | null;
  /** null = the solve-state source did not answer (DD #1 `solveStateAvailable = false`). */
  solveState: SolveState | null;
  /** null = not asked: the badge comes from the AI module (F5), which is not built; the badge is not drawn. */
  hasSolutionReview: boolean | null;
};

/** Filters, sort and paging of the catalogue; every value is already normalised by the screen. */
export type CatalogParams = {
  q: string;
  status: SolveState | "all";
  /** A level code, or "all". */
  difficulty: string;
  /** A topic id, or "all". */
  topicId: string;
  sort: "difficulty" | "acRate";
  dir: "asc" | "desc";
  page: number;
  pageSize: number;
};

export type CatalogPage = {
  items: CatalogRow[];
  page: PageMeta;
  /** false = the status filter was ignored and every `solveState` is null. */
  solveStateAvailable: boolean;
};

export type TopicChip = {
  id: string;
  name: string;
  /** null = unknown (solve state unavailable); the chip then shows the total only. */
  solved: number | null;
  total: number;
};

export type TopicProgress = {
  items: TopicChip[];
  solveStateAvailable: boolean;
};

export type AssignmentRow = {
  problemId: string;
  title: string;
  solveState: SolveState | null;
  /** ISO-8601 UTC. */
  assignedAt: string;
};

export type AssignmentGroup = {
  classId: string;
  className: string;
  items: AssignmentRow[];
};

/** One row of the learner's class problems (#13A); a withdrawn problem keeps its row but cannot be opened. */
export type ClassProblemRow = {
  classId: string;
  problemId: string;
  code: string;
  title: string;
  levelDisplayName: string;
  solveState: SolveState;
  /** ISO-8601 UTC. */
  assignedAt: string;
  withdrawn: boolean;
};

export type ClassProblemsPage = {
  items: ClassProblemRow[];
  page: PageMeta;
  /** Totals over every page of the same filter; `solvedCount` null = the solve-state source did not answer. */
  meta: { totalAssigned: number; solvedCount: number | null };
};

export type { InProgressProblem };
