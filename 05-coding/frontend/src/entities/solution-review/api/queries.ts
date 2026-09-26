// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Mirrors entities/admin-dashboard/api/queries.ts: one useQuery hook through withMockData, so
// graduating to the real endpoint (03-dd/api/ai-review.md) only means deleting the __mock__ import.
import { useQuery } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import { fetchSolutionReviewMock, type SolutionReviewDemoOutcome } from "./__mock__/solution-review-mocks";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/ai-review.md does not define this endpoint yet");
}

export function useSolutionReview(demo: SolutionReviewDemoOutcome) {
  return useQuery({
    queryKey: ["solution-review", demo],
    queryFn: () => withMockData(() => fetchSolutionReviewMock(demo), notImplemented),
    retry: false,
    staleTime: 30_000,
  });
}
