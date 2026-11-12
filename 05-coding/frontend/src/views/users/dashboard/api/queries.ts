// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { getRealRecentReviews } from "@/entities/solution-review";
import { withMockData } from "@/shared/api";
import type { DashboardRange, RecentSolutionReview } from "../model/types";
import {
  fakeGetActivityCalendar,
  fakeGetDashboardDaily,
  fakeGetRecentSolutionReviews,
} from "./__mock__/student-dashboard-mocks";
import { realGetActivityCalendar } from "./real-activity-calendar";
import { realGetDashboardDaily } from "./real-dashboard-daily";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

export const useDashboardDaily = (range: DashboardRange) =>
  useQuery({
    queryKey: ["student-dashboard", "daily", range],
    queryFn: () =>
      withMockData(
        () => fakeGetDashboardDaily(range),
        () => realGetDashboardDaily(range),
      ),
    ...QUERY_OPTIONS,
  });

export const useActivityCalendar = () =>
  useQuery({
    queryKey: ["student-dashboard", "activity"],
    queryFn: () =>
      withMockData(() => fakeGetActivityCalendar(), realGetActivityCalendar),
    ...QUERY_OPTIONS,
  });

/**
 * ai-review-owned, so it gets its own query rather than riding along with the identity-owned
 * blocks: when F5 is down or out of quota this one fails alone and the rest of the screen keeps
 * rendering (REQ-07 of 01-rd/screens/users/USR0601_dashboard.md; the graceful-degradation rule in
 * CLAUDE.md).
 */
export const useRecentSolutionReviews = () =>
  useQuery({
    queryKey: ["student-dashboard", "recent-reviews"],
    // F5 #7 (`GET /ai/me/solution-reviews/recent`, server default of 3 rows). `reviewedAt` stays an ISO
    // instant; the block formats it.
    queryFn: () =>
      withMockData(fakeGetRecentSolutionReviews, async () =>
        (await getRealRecentReviews()).map(
          (item): RecentSolutionReview => ({
            submissionId: item.submissionId,
            problemTitle: item.problemTitle,
            reviewedAt: item.createdAt,
            complexity: { actualTime: item.timeComplexity },
            verdict: item.verdict,
          }),
        ),
      ),
    ...QUERY_OPTIONS,
  });
