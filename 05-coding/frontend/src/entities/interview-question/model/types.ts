/**
 * Interview question bank (F6-13, DEC-2026-0830-interview-bank-crud).
 *
 * FIVE topics, replacing an earlier stale set of four — that decision settles the taxonomy.
 * A2 and A3 can edit the whole bank, not only questions they authored themselves.
 */
export type QuestionTopic = "csTheory" | "systemDesign" | "database" | "language" | "behavioural";

export const QUESTION_TOPICS: QuestionTopic[] = [
  "csTheory",
  "systemDesign",
  "database",
  "language",
  "behavioural",
];

export type QuestionLevel = "easy" | "medium" | "hard";

export const QUESTION_LEVELS: QuestionLevel[] = ["easy", "medium", "hard"];

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
};

export type QuestionStat = {
  key: string;
  value: string;
  delta: string;
  deltaColorVar: string;
};

export type InterviewQuestionPage = {
  stats: QuestionStat[];
  questions: InterviewQuestion[];
  totalQuestions: number;
};
