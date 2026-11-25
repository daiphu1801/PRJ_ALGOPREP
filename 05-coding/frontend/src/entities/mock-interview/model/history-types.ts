// Types of the learner's session history (F5 section 31A: the list; section 9: one session in detail).
import type { PageMeta } from "@/shared/api";
import type { InterviewEntryType, InterviewerLevel } from "./types";

export type HistoryStatus = "completed" | "abandoned";
export type HistoryScoreBand = "weak" | "fair" | "good";

export type InterviewHistoryItem = {
  sessionId: string;
  entryType: InterviewEntryType;
  status: HistoryStatus;
  /** Only a bank-question session carries the question text. */
  questionText: string | null;
  /** Only a topic-selection session carries topics. */
  topics: string[];
  /** `null` for a session started from a submission (it has no level setting). */
  level: InterviewerLevel | null;
  turnCount: number;
  maxTurns: number | null;
  startedAt: string;
  endedAt: string;
  /** `null` when the session was abandoned or was never fully scored. */
  overallScore: number | null;
};

export type InterviewHistoryQuery = {
  q: string;
  entryType: "all" | InterviewEntryType;
  status: "all" | HistoryStatus;
  level: "all" | InterviewerLevel;
  scoreBand: "all" | HistoryScoreBand;
  sort: "startedAt" | "score";
  order: "asc" | "desc";
  page: number;
  size: number;
};

export type InterviewHistoryPage = {
  items: InterviewHistoryItem[];
  page: PageMeta;
};

export const DEFAULT_HISTORY_QUERY: InterviewHistoryQuery = {
  q: "",
  entryType: "all",
  status: "all",
  level: "all",
  scoreBand: "all",
  sort: "startedAt",
  order: "desc",
  page: 1,
  size: 10,
};
