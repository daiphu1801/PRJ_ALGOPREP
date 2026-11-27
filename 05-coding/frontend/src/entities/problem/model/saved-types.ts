// Saved problems as the real api answers them (03-dd/api/F2_problem-bank.md #8 to #10, DD sections 9 to 11).
import type { PageMeta } from "@/shared/api";
import type { SolveState } from "./types";

export type SavedRow = {
  id: string;
  code: string;
  title: string;
  /** The level's code, the same key `useProblemLevels` lists. */
  difficulty: string;
  topics: string[];
  /** null = the solve-state source did not answer; the row then shows no status. */
  solveState: SolveState | null;
  /** Private note, absolute per (user_id, problem_id), never shared (F2-13); "" when there is none. */
  note: string;
  /** ISO-8601 UTC; the view prints it in UTC+7. */
  savedAt: string;
  /** Unpublished or soft-hidden after the learner saved it (USR0103 Q6): still listed, cannot be opened. */
  withdrawn: boolean;
};

/** Filters and paging the screen sends. The list is always newest-saved first and 20 rows a page. */
export type SavedParams = {
  q: string;
  status: SolveState | "all";
  /** A level code, or "all". */
  difficulty: string;
  page: number;
};

export type SavedPage = {
  items: SavedRow[];
  page: PageMeta;
  meta: {
    /** Every bookmark of the caller, withdrawn ones included, before any filter. */
    savedTotal: number;
    /** Bookmarks the caller solved; null when the solve state is unavailable. */
    solvedCount: number | null;
    /** Rows that match the filter. */
    filteredTotal: number;
  };
};

export type SaveNoteInput = { problemId: string; note: string };

/** What "Hoàn tác" needs to put an unsaved bookmark back together with its note. */
export type RestoreInput = { problemId: string; note: string };
