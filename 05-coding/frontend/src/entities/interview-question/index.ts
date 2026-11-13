export {
  RECALL_LEVELS,
  RECALL_NEXT_REVIEW_DAYS,
  type AnswerAttempt,
  type AnswerCriterionResult,
  type AnswerAttemptFeedbackStatus,
  type InterviewQuestion,
  type InterviewQuestionPage,
  type QuestionLevel,
  type QuestionTopic,
  type RecallLevel,
  type RubricCriterion,
} from "./model/types";
export { topicLabel, type InterviewTopic } from "./model/topic-store";
export {
  levelLabel,
  levelTone,
  type InterviewLevel,
} from "./model/level-store";
export {
  useInterviewLevelActions,
  useInterviewLevels,
  useInterviewTopicActions,
  useInterviewTopics,
  type ManagedActions,
  type TopicActions,
} from "./api/catalog-queries";
export {
  CSV_TEMPLATE,
  parseQuestionsCsv,
  type CsvImportParse,
  type CsvRowError,
  type QuestionDraft,
} from "./model/csv-import";
export type {
  DrillCard,
  LearnerQuestionDetail,
  LearnerQuestionFilters,
  LearnerQuestionItem,
  LearnerQuestionPage,
  PracticeAttempt,
  PracticeAttemptPage,
  QuestionRecall,
  RecallStatusFilter,
} from "./model/learner-types";
export { splitOutline } from "./lib/split-outline";
export { nextReviewDays } from "./lib/next-review";
export { resetRecallAndBookmarkState } from "./api/__mock__/learner-mock";
export { InterviewQuestionCard } from "./ui/interview-question-card";
export { InterviewQuestionListRow } from "./ui/interview-question-list-row";
export { RecallLevelPicker } from "./ui/recall-level-picker";
export { BookmarkToggle } from "./ui/bookmark-toggle";
export {
  isQuestionGone,
  useDropUnavailableQuestion,
  useLearnerQuestionDetail,
  useLearnerQuestionList,
  usePracticeAttempts,
  useRateRecall,
  useSetBookmark,
  useStartDrill,
  useSubmitPracticeAnswer,
} from "./api/learner-queries";
export { DRILL_SIZE } from "./api/real-learner";
export {
  useImportInterviewQuestions,
  useInterviewQuestion,
  useInterviewQuestionPage,
} from "./api/queries";

// --- Admin question management (SHR0301/0302/0303, F6 #19-#24): hooks and wire types. ---
export {
  useAdminQuestion,
  useAdminQuestionList,
  useCreateAdminQuestion,
  useImportAdminQuestions,
  useQuestionUsage,
  useRetireAdminQuestions,
  useUpdateAdminQuestion,
} from "./api/admin-queries";
export type {
  AdminListParams,
  AdminQuestionDetail,
  AdminQuestionInput,
  AdminQuestionList,
  AdminQuestionRow,
  AdminRubricItem,
  AdminSortKey,
  ImportResult,
  ImportRowInput,
  RetireResult,
} from "./api/real-admin-questions";

// --- Dashboard topic progress (USR0601, F6 #8). ---
export { useInterviewTopicProgress } from "./api/progress-queries";
export type { InterviewTopicProgress } from "./api/progress-queries";
