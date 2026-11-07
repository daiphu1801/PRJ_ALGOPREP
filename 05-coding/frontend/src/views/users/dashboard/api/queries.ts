// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import type { DashboardRange } from "../model/types";
import {
  fakeGetActivityCalendar,
  fakeGetDashboardDaily,
  fakeGetRecentSolutionReviews,
} from "./__mock__/student-dashboard-mocks";
import { realGetActivityCalendar } from "./real-activity-calendar";
import { realGetDashboardDaily } from "./real-dashboard-daily";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

// The only call left on mock: Solution Review recent block (F5, no controller yet).
function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/F5_ai-review.md: recent solution reviews are not built yet",
  );
}

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
    queryFn: () => withMockData(fakeGetRecentSolutionReviews, notImplemented),
    ...QUERY_OPTIONS,
  });
