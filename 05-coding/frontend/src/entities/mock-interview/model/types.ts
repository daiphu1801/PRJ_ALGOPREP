// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// Types follow 02-bd/screens/users/USR0302_mock_interview.md Sheet 4.2/5. Note this is a DIFFERENT
// shape from entities/solution-review despite both belonging to `ai-review` — F5.1 is a one-shot
// JSON report, F5.2 (this one) is a multi-turn conversation, per PROTOTYPE_DEBT.md section 10.2's
// closing note on why the two screens are not merged.

export type InterviewEntryType = "submission" | "bank" | "custom";

/** 4 levels, unified system-wide — DEC-2026-0922-users-and-admin-conflict-resolutions. */
export type InterviewerLevel = "intern" | "junior" | "middle" | "senior";

export type InterviewStage = "explain" | "challenge" | "scaleUp";

export type ChatRole = "ai" | "user";

export type ChatMessage = {
  id: string;
  role: ChatRole;
  content: string;
  stage: InterviewStage;
};

/** 4 fixed criteria, fixed weights — 02-bd/database/ai-review.md:96-97. */
export type RubricCriterionCode =
  | "clarity"
  | "technicalAccuracy"
  | "pushbackHandling"
  | "complexityAwareness";

export const RUBRIC_WEIGHTS: Record<RubricCriterionCode, number> = {
  clarity: 25,
  technicalAccuracy: 30,
  pushbackHandling: 25,
  complexityAwareness: 20,
};

export const RUBRIC_CRITERION_CODES: RubricCriterionCode[] = [
  "clarity",
  "technicalAccuracy",
  "pushbackHandling",
  "complexityAwareness",
];

export type RubricScore = {
  code: RubricCriterionCode;
  score: number;
  comment: string;
};

export type InterviewResult = {
  overallScore: number;
  criteria: RubricScore[];
  feedbackSummary: string;
  strengths: string[];
  improvements: string[];
};

export type AcceptedSubmissionOption = {
  id: string;
  problemTitle: string;
  language: string;
};

export type BankQuestionOption = {
  id: string;
  title: string;
};

export type EntryStats = {
  sessionsCompleted: number;
  averageScore: number | null;
  weakestCriterion: RubricCriterionCode | null;
};

export type MaxTurns = 8 | 12 | 16;

export const MAX_TURNS_OPTIONS: MaxTurns[] = [8, 12, 16];

export const INTERVIEWER_LEVELS: InterviewerLevel[] = ["intern", "junior", "middle", "senior"];

export type SessionConfig = {
  entryType: InterviewEntryType;
  submissionId: string | null;
  questionId: string | null;
  customTopics: string[];
  level: InterviewerLevel;
  maxTurns: MaxTurns;
  hintAllowed: boolean;
};
