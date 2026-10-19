import type { BadgeVariant } from "@/shared/ui";
import type { SubmissionStatus } from "./types";

/** Badge tone of a submission verdict, shared by the learner result, the instructor result and history tables. */
export const SUBMISSION_STATUS_VARIANT: Record<SubmissionStatus, BadgeVariant> =
  {
    ACCEPTED: "success",
    WRONG_ANSWER: "negative",
    TIME_LIMIT_EXCEEDED: "warn",
    SYSTEM_ERROR: "warn",
    RUNTIME_ERROR: "negative",
    COMPILE_ERROR: "warn",
    PENDING: "neutral",
    COMPILING: "neutral",
    JUDGING: "cyan",
  };
