/**
 * Learner-side shapes of the question bank (USR0401, USR0402; 03-dd/api/F6_interview-bank.md #2 to #7).
 * Kept apart from the admin `InterviewQuestion`: a learner never gets `rubric`, usage figures or a
 * display code. `id` is the server UUID (in mock mode the mock's `IQ-nnn` code stands in for it);
 * `topic` and `level` are the catalogue codes, so `topicLabel` and `levelLabel` resolve them.
 */
import type {
  AnswerAttempt,
  QuestionLevel,
  QuestionTopic,
  RecallLevel,
} from "./types";

export type RecallStatusFilter = "all" | "new" | "reviewing" | "known";

/** What the list screen filters by; the server does the filtering and paging. */
export type LearnerQuestionFilters = {
  /** Already trimmed and capped to 100 characters by the caller. */
  q: string;
  status: RecallStatusFilter;
  /** Topic code, or `"all"`. */
  topic: string;
  /** Level code, or `"all"`. */
  level: string;
  bookmarkedOnly: boolean;
};

export type LearnerQuestionItem = {
  id: string;
  topic: QuestionTopic;
  level: QuestionLevel;
  title: string;
  recall: RecallLevel | null;
  bookmarked: boolean;
};

/** A drill card is a list item plus the two fields the flashcard shows. */
export type DrillCard = LearnerQuestionItem & {
  suggestedApproach: string[];
  coreKeywords: string[];
};

export type LearnerQuestionPage = {
  items: LearnerQuestionItem[];
  /** 1-based number of this page. */
  number: number;
  totalPages: number;
  /** Questions matching the filters / questions in the whole bank (`meta` of the server answer). */
  shown: number;
  total: number;
};

export type QuestionRecall = {
  level: RecallLevel;
  intervalDays: number;
  /** ISO datetime. */
  nextReviewAt: string;
  ratedAt: string;
};

export type LearnerQuestionDetail = {
  id: string;
  topic: QuestionTopic;
  usesStarFramework: boolean;
  level: QuestionLevel;
  title: string;
  content: string;
  suggestedApproach: string[];
  sampleAnswerFramework: string;
  coreKeywords: string[];
  followUps: string[];
  bookmarked: boolean;
  /** At least one rubric row exists, so Practice mode is unlocked. */
  practiceAvailable: boolean;
  recall: QuestionRecall | null;
};

/**
 * One practice attempt. `feedbackErrorCode` is only present on the answer of the submit call
 * (INB-501 or INB-502); a re-read of the history carries none (DD 1.3 "AnswerAttemptDto").
 */
export type PracticeAttempt = AnswerAttempt & {
  feedbackErrorCode?: string | null;
};

export type PracticeAttemptPage = {
  items: PracticeAttempt[];
  hasMore: boolean;
  totalAttempts: number;
};
