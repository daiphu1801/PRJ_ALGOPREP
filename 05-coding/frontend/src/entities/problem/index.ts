// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
export {
  useWorkspaceProblem,
  useEnabledLanguages,
  useStarterCode,
} from "./api/workspace-queries";
export type {
  ClassAssignmentGroup,
  ClassAssignmentItem,
  Difficulty,
  InProgressProblem,
  InputMethod,
  OverallVerdict,
  ProblemDetail,
  ProblemExample,
  ProblemListFilter,
  ProblemListItem,
  ProblemListPage,
  RatioStat,
  SampleRunResult,
  SavedProblem,
  SolutionReviewQuickInsight,
  SolveState,
  StarterCodeKey,
  SubmissionHistoryItem,
  SubmissionLanguage,
  SubmissionModel,
  SubmissionRunResult,
  TestcaseVerdict,
  TopicProgress,
  WrapperMode,
} from "./model/types";
export {
  saveWorkspaceDraft,
  workspaceDraftKey,
  type DraftModel,
} from "./model/workspace-draft";
export {
  AI_GUARD_KEYS,
  TESTCASE_CATEGORIES,
  type AiGuardKey,
  type DroppedReason,
  type GenerateTestcasesInput,
  type GenerateTestcasesResult,
  type ProblemDraft,
  type ProblemDraftRecord,
  type ProblemStats,
  type ServerState,
  type ProblemLimits,
  type Testcase,
  type TestcaseCategory,
  type TestcaseOrigin,
  type TestcaseVisibility,
  type WorkedExample,
} from "./model/draft-types";
export {
  realBulkUpload,
  realCreateTestcase,
  realDeleteTestcase,
  realRunReferenceSolution,
  realUpdateTestcase,
  type ReferenceRun,
  type SaveResult,
  type SaveSection,
} from "./api/real-authoring";
export {
  useCreateSubmission,
  useGenerateTestcases,
  useProblemDetail,
  useProblemDraft,
  useProblemListPage,
  usePublishProblem,
  useRunSampleTestcases,
  useSaveProblemDraft,
} from "./api/queries";
export {
  addProblemTopic,
  problemTopicLabel,
  moveProblemTopic,
  removeProblemTopic,
  renameProblemTopic,
  type ProblemTopic,
} from "./model/topic-store";
export {
  addProblemLevel,
  getProblemLevels,
  moveProblemLevel,
  problemLevelLabel,
  problemLevelRank,
  problemLevelTone,
  removeProblemLevel,
  renameProblemLevel,
  type ProblemLevel,
  type ProblemLevelError,
} from "./model/level-store";
export {
  AI_GENERATION_LIMIT_MAX,
  AI_GENERATION_LIMIT_MIN,
  aiGenerationSettings,
} from "./model/generation-settings-store";
export { canViewerSeeProblem } from "./model/mock-ownership";
export {
  listCreatedProblems,
  type CreatedProblem,
} from "./model/created-problems";
export {
  allowsUnorderedSet,
  defaultType,
  derivedName,
  elementKinds,
  epsilonValid,
  exceedsSchema,
  isIdentifier,
  isSpecComplete,
  isTypeValid,
  languageName,
  matchingValid,
  renderSignature,
  signatureIssues,
} from "./model/spec";
export {
  CONTAINER_KINDS,
  MAX_TYPE_NESTING,
  MATCHING_STRATEGIES,
  SCALAR_KINDS,
  SPEC_LANGUAGES,
  TYPE_KINDS,
  type FunctionSignature,
  type MatchingStrategy,
  type ProblemSpec,
  type ScalarKind,
  type SpecLanguage,
  type SpecParameter,
  type SpecType,
  type TypeKind,
} from "./model/draft-types";
export {
  useProblemLevelActions,
  useProblemLevels,
  useProblemTopicActions,
  useProblemTopics,
  type ManagedActions,
} from "./api/catalog-queries";
export {
  IN_PROGRESS_LIMIT,
  useMyClassAssignments,
  useMyClassProblems,
  useMyInProgress,
  useProblemCatalog,
  useRandomProblem,
  useSuggestedProblems,
  useTopicProgress,
} from "./api/student-catalog-queries";
export type {
  AssignmentGroup,
  AssignmentRow,
  CatalogPage,
  ClassProblemRow,
  ClassProblemsPage,
  CatalogParams,
  CatalogRow,
  TopicChip,
  TopicProgress as TopicProgressPage,
} from "./model/catalog-types";
/** Test hook of the mock list: back to the literal data. */
export { resetSavedMock } from "./api/__mock__/saved-problems-mocks";
export {
  useRestoreBookmark,
  useSaveBookmarkNote,
  useSavedProblemsPage,
  useSetBookmark,
  useUnsaveProblem,
} from "./api/saved-queries";
export type {
  RestoreInput,
  SaveNoteInput,
  SavedPage,
  SavedParams,
  SavedRow,
} from "./model/saved-types";
export { InProgressList } from "./ui/in-progress-list";
export { overallVerdict } from "./api/real-execution";
