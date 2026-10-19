import type { SubmissionLanguage, SubmissionStatus } from "./types";

/** Result filter of the instructor submission tables (student history, submission list). */
export type SubmissionResultFilter = "all" | "accepted" | "failed";

export const SUBMISSION_LANGUAGE_LABEL: Record<SubmissionLanguage, string> = {
  PYTHON: "Python",
  JAVA: "Java",
  CPP: "C++",
};

// "failed" = judged and not accepted; a submission still being judged, or ended by a system error
// (not the learner's fault), is only under "all" (DEC-2026-1014-instructor-submissions-list-details).
const FAILED_STATUSES: SubmissionStatus[] = [
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
  "COMPILE_ERROR",
];

export function matchesResultFilter(
  status: SubmissionStatus,
  filter: SubmissionResultFilter,
): boolean {
  if (filter === "all") return true;
  if (filter === "accepted") return status === "ACCEPTED";
  return FAILED_STATUSES.includes(status);
}
