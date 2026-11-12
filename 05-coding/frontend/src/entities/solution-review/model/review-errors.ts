// Error codes the solution-review screen branches on (03-dd/api/F5_ai-review.md section 4.4, DD
// USR0301 table 3.2). They belong to the API contract, not to the mock, so the view imports them
// from the entity and not from `__mock__`.

/** Unknown or malformed submission id. */
export const AIR_SUBMISSION_NOT_FOUND = "AIR-101";
/** Submission is not ACCEPTED, so no review exists. */
export const AIR_NOT_ACCEPTED = "AIR-102";
/** Another call is generating the report for this submission right now (409). */
export const AIR_IN_PROGRESS = "AIR-103";
/** Someone else's submission. The screen merges it with 101 and 102 on purpose (no existence leak). */
export const AIR_NOT_OWNER = "AIR-302";
/** Token budget exhausted: no retry button. */
export const AIR_BUDGET_LOCKED = "AIR-401";
/** Hourly quota and cooldown: retry after `details.retryAfterSeconds`. */
export const AIR_QUOTA_EXCEEDED = "AIR-402";
export const AIR_COOLDOWN = "AIR-404";
/** Provider unavailable or answer with the wrong shape: transient, retry allowed. */
export const AIR_PROVIDER_UNAVAILABLE = "AIR-501";
export const AIR_INVALID_RESPONSE = "AIR-502";

/** `?demo=` query param of the prototype: picks which outcome the mock plays. */
export type SolutionReviewDemoOutcome =
  "ready" | "errorTransient" | "errorBudgetLocked";
