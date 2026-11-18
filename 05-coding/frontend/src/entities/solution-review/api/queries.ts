// Data boundary of F5 Solution Review (03-dd/api/F5_ai-review.md sections 4 to 7). Mock = the fixture
// in __mock__/solution-review-mocks.ts; real = real-solution-review.ts.
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isMockMode, withMockData } from "@/shared/api";
import type { SolutionReviewDemoOutcome } from "../model/review-errors";
import { fetchSolutionReviewMock } from "./__mock__/solution-review-mocks";
import {
  getRealReviewQuickInsight,
  getRealReviewedProblemIds,
  getRealSolutionReview,
} from "./real-solution-review";

const REVIEW_DEPENDENT_KEYS = [
  ["solution-review", "problem-ids"],
  ["solution-review", "quick-insight"],
  ["student-dashboard", "recent-reviews"],
] as const;

/**
 * BR-SC-01: a stored report never changes and a failed call can cost tokens, so nothing here
 * refetches by itself (`staleTime: Infinity`, no retry, no refetch on focus or reconnect); only the
 * Retry button calls again. The hook uses `useQuery` + `withMockData` (not `useMockQuery`) to get the abort `signal`.
 */
export const useSolutionReview = (
  submissionId: string,
  demo: SolutionReviewDemoOutcome,
) => {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: ["solution-review", submissionId, demo],
    // `signal` aborts the in-flight call when the learner leaves the screen (BR-SC-01 step 5).
    queryFn: ({ signal }) =>
      withMockData(
        () => fetchSolutionReviewMock(demo, submissionId),
        async () => {
          const review = await getRealSolutionReview(submissionId, signal);
          // A first report changes the "analysed" label, the quick insight and the dashboard block.
          for (const queryKey of REVIEW_DEPENDENT_KEYS) {
            void queryClient.invalidateQueries({ queryKey });
          }
          return review;
        },
      ),
    staleTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    enabled: Boolean(submissionId),
  });
};

/**
 * #5: ids of problems the caller has a review for (label in USR0101). Real mode only: mock rows carry
 * their own `hasSolutionReview`. F5 failing leaves the set empty so the list keeps rendering.
 */
export const useReviewedProblemIds = () =>
  useQuery({
    queryKey: ["solution-review", "problem-ids"],
    queryFn: async () => new Set(await getRealReviewedProblemIds()),
    enabled: !isMockMode(),
    staleTime: 30_000,
    retry: false,
  });

/** #6: latest review of one problem (USR0102, USR0104). Real mode only; null when none exists. */
export const useReviewQuickInsight = (problemId: string | undefined) =>
  useQuery({
    queryKey: ["solution-review", "quick-insight", problemId],
    queryFn: () => getRealReviewQuickInsight(problemId ?? ""),
    enabled: !isMockMode() && Boolean(problemId),
    staleTime: 30_000,
    retry: false,
  });
