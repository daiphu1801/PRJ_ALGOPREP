/**
 * Interview question bank (F6-13, DEC-2026-0830-interview-bank-crud).
 *
 * A2 and A3 can edit the whole bank, not only questions they authored themselves.
 * Topics and difficulty levels are admin-managed data (DEC-2026-1001-admin-configurable-settings), so a
 * question carries only their stable keys; labels come from `useInterviewTopics()` / `useInterviewLevels()`.
 */
export type QuestionTopic = string;

/** Stable key of an admin-managed difficulty level (see ./level-store). */
export type QuestionLevel = string;

export type RubricCriterion = {
  label: string;
  /** Weight percentage, 0-100. */
  weight: number;
};

export type InterviewQuestion = {
  /** Display code, e.g. "IQ-014". Doubles as the key. */
  code: string;
  topic: QuestionTopic;
  level: QuestionLevel;
  question: string;
  /** Follow-up probes the interviewer can use. */
  followUps: string[];
  rubric: RubricCriterion[];
  usageCount: number;
  /** Average score out of 5, e.g. 3.8. */
  averageScore: number;

  // --- Student-facing fields added for USR0401/USR0402 (02-bd/screens/users/USR0401_*.md
  // section 4.2, USR0402_*.md section 4.2). Admin-facing screen (interview-question-management)
  // does not read these — kept optional-in-spirit but always populated by the mock so both
  // screens can rely on them without an `?`.

  /** "Nội dung câu hỏi" (content_markdown) — full body, shown only on USR0402 detail. */
  content: string;
  /** "Ý cần nói" (suggested_approach) — distinct from `followUps` ("câu hỏi đào sâu"). */
  suggestedApproach: string[];
  /** "Từ khoá cốt lõi" (core_keywords). */
  coreKeywords: string[];
  /** "Khung trả lời chuẩn" (sample_answer_framework) — Markdown, STAR structure for `behavioural`. */
  sampleAnswerFramework: string;
  /** Whether Practice mode is unlocked (mirrors `practiceAvailable` — has >=1 rubric row). */
  hasRubric: boolean;
  /**
   * Seed value for the student's own recall self-rating (F6-12). [SoT: Suy luận] — prototype has
   * no per-user backing store, so this seeds `useRecallAndBookmarkState`'s initial map.
   */
  defaultRecall: RecallLevel | null;
  /** Practice attempt history (F6-09), newest first. Mock-seeded, USR0402 only. */
  attempts: AnswerAttempt[];
};

// F6-12 self-rating, matches enum `recall_ratings.rating` (KNOWN/VAGUE/FORGOTTEN) —
// [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md — Sheet 5 mục D.5, Câu hỏi mở Q6].
export type RecallLevel = "known" | "vague" | "forgotten";

export const RECALL_LEVELS: RecallLevel[] = ["known", "vague", "forgotten"];

export type AnswerAttemptFeedbackStatus = "pending" | "completed" | "failed";

export type AnswerAttempt = {
  attemptNo: number;
  /** ISO datetime string. */
  createdAt: string;
  feedbackStatus: AnswerAttemptFeedbackStatus;
  answerText: string;
  strengths?: string[];
  gaps?: string[];
  nextSteps?: string;
};

export type InterviewQuestionPage = {
  questions: InterviewQuestion[];
  totalQuestions: number;
};
