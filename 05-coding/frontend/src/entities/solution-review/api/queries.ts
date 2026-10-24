// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Mirrors entities/admin-dashboard/api/queries.ts: one useQuery hook through withMockData, so
// graduating to the real endpoint (03-dd/api/F5_ai-review.md) only means deleting the __mock__ import.
import { useMockQuery } from "@/shared/api";
import type { SolutionReviewDemoOutcome } from "../model/review-errors";
import { fetchSolutionReviewMock } from "./__mock__/solution-review-mocks";

export const useSolutionReview = (
  submissionId: string,
  demo: SolutionReviewDemoOutcome,
) =>
  useMockQuery({
    queryKey: ["solution-review", submissionId, demo],
    mock: () => fetchSolutionReviewMock(demo, submissionId),
    spec: "03-dd/api/F5_ai-review.md",
  });
