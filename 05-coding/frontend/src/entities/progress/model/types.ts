// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Domain types for USR0501_my_progress, one per DTO in
// 02-bd/screens/users/USR0501_my_progress.md Sheet 7.1. New entity (not entities/user) because this
// is read-only aggregate data spanning `identity` + `ai-review`, not the account record itself.
import { z } from "zod";

export const progressRangeSchema = z.enum(["7d", "30d", "all"]);
export type ProgressRange = z.infer<typeof progressRangeSchema>;

/** `MyProgressOverviewDto` — global figures, NOT affected by the range filter [SoT: Sheet 6 Khu vực A]. */
export type ProgressOverview = {
  solvedProblemCount: number;
  totalPublishedProblemCount: number;
  totalSubmissions: number;
  acceptedCount: number;
  currentStreakDays: number;
  /** `null` when `solvedProblemCount === 0` — the screen shows "-" instead of dividing by zero. */
  avgAttemptsPerSolved: number | null;
};

/** `TopicProgressDto`. `solvedCount`/`totalCount` are cumulative (range-independent); `acRate`/`lastSubmittedAt` follow the range filter [SoT: Sheet 6 Khu vực B]. */
export type TopicProgress = {
  topicId: string;
  topicName: string;
  solvedCount: number;
  totalCount: number;
  /** `null` when there is no submission in the selected range. */
  acRate: number | null;
  /** ISO date, or `null`. */
  lastSubmittedAt: string | null;
};

/** Key of an admin-managed problem level (entities/problem/model/level-store). */
export type Difficulty = string;

/** `DifficultyProgressDto` — always cumulative, never range-filtered. */
export type DifficultyProgress = {
  difficulty: Difficulty;
  solvedCount: number;
  totalCount: number;
};

/** `SubmissionDailyCountDto` — always 14 entries, zero-filled days included. */
export type SubmissionDailyCount = {
  day: string;
  count: number;
};

/** `FocusSuggestionDto` — derived from `TopicProgress`, no dedicated endpoint (Sheet 7.1 NO 13). */
export type FocusReason =
  | { code: "lowestAcRate"; acRate: number }
  | { code: "mostUnsolved"; remaining: number; acRate: number }
  | { code: "leastRecentlySubmitted"; daysSince: number };

export type FocusSuggestion = {
  topicId: string;
  topicName: string;
  reason: FocusReason;
};

export const RUBRIC_CRITERIA = ["CLARITY", "TECHNICAL_ACCURACY", "PUSHBACK_HANDLING", "COMPLEXITY_AWARENESS"] as const;
export type RubricCriterionCode = (typeof RUBRIC_CRITERIA)[number];

export type RecentInterviewSession = {
  sessionId: string;
  startedAt: string;
  /** `null` when the session has no rubric yet. */
  score: number | null;
};

/** `MyInterviewSummaryDto` — ai-review-owned, degrades independently of the rest of the screen. */
export type InterviewSummary = {
  completedSessionCount: number;
  /** `null` when there is no session with a rubric yet. */
  averageScore: number | null;
  weakestCriterionCode: RubricCriterionCode | null;
  recentSessions: RecentInterviewSession[];
};
