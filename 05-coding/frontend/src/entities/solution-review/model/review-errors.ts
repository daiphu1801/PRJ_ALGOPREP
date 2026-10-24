// Error codes the solution-review screen branches on (BD USR0301 Sheet 9). They belong to the API
// contract, not to the mock, so the view imports them from the entity and not from `__mock__`.
export const AI_BUDGET_LOCKED_CODE = "AI_BUDGET_LOCKED";
export const AI_PROVIDER_TIMEOUT_CODE = "AI_PROVIDER_TIMEOUT";
/** Unknown id, or one the caller does not own (BD Sheet 9 NO 2); the view shows one notice for both. */
export const REVIEW_NOT_FOUND_CODE = "NOT_FOUND";
export const REVIEW_FORBIDDEN_CODE = "FORBIDDEN";
/** Submission is not ACCEPTED, so no review exists (BD Sheet 9 NO 3). */
export const REVIEW_NOT_ACCEPTED_CODE = "NOT_ACCEPTED";

/** `?demo=` query param of the prototype: picks which outcome the mock plays. */
export type SolutionReviewDemoOutcome =
  "ready" | "errorTransient" | "errorBudgetLocked";
