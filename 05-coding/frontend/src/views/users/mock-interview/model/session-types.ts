// The one shape the screen reads. The mock hook and the real hook both return it, so the view has
// no idea which one runs (mock mode keeps working unchanged).
import type {
  ChatMessage,
  InterviewEntryType,
  InterviewResult,
  InterviewStage,
  InterviewerLevel,
  MaxTurns,
} from "@/entities/mock-interview";

export type ScreenState = "entry" | "running" | "result";

/** Sub-states of `running` (DD screen table 3.1). The mock only ever uses `idle`. */
export type RunState =
  | "restoring"
  | "idle"
  | "sending"
  | "streaming"
  | "waiting"
  | "failedTurn"
  | "completionPending"
  | "completing";

export type InterviewSession = {
  screenState: ScreenState;
  runState: RunState;
  /** Waiting for the opening message of a new session; the screen stays on `entry`. */
  starting: boolean;
  /** `waiting` lasted the whole polling budget: say it is slow, never call it an error. */
  waitingSlow: boolean;
  /** Hints are offered only when the session allows them (never for a SUBMISSION entry). */
  hintOffered: boolean;
  hintPending: boolean;
  entryType: InterviewEntryType;
  setEntryType: (value: InterviewEntryType) => void;
  submissionId: string | null;
  setSubmissionId: (value: string | null) => void;
  questionId: string | null;
  setQuestionId: (value: string | null) => void;
  customTopics: string[];
  toggleCustomTopic: (key: string) => void;
  level: InterviewerLevel;
  setLevel: (value: InterviewerLevel) => void;
  maxTurns: MaxTurns;
  setMaxTurns: (value: MaxTurns) => void;
  hintAllowed: boolean;
  setHintAllowed: (value: boolean) => void;
  canStart: boolean;
  start: () => void;
  messages: ChatMessage[];
  isAiTyping: boolean;
  currentStage: InterviewStage;
  turnCount: number;
  /** Null for a SUBMISSION session: the ceiling is server side and not shown. */
  turnLimit: number | null;
  elapsedSeconds: number;
  /** Resolves true when the answer was taken; false means the screen puts the draft back. */
  sendMessage: (text: string) => Promise<boolean>;
  requestHint: () => void;
  stopEarly: () => void;
  retryTurn: () => void;
  retryComplete: () => void;
  retryPractice: () => void;
  result: InterviewResult | null;
};
