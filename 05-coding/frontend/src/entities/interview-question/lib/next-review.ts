const DAY_MS = 86_400_000;

/**
 * Whole days from `now` to the server's `nextReviewAt`, rounded up (USR0401 BR-SC-04). A result of 0
 * or less means the review is already due, and the screen says so instead of "0 ngày" [SoT: Suy luận].
 */
export function nextReviewDays(
  nextReviewAt: string,
  now: number = Date.now(),
): number {
  return Math.ceil((new Date(nextReviewAt).getTime() - now) / DAY_MS);
}
