// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import type { ProgressRange } from "../model/types";
import {
  fakeGetDifficultyBreakdown,
  fakeGetInterviewSummary,
  fakeGetProgressOverview,
  fakeGetTopicProgress,
} from "./__mock__/progress-mocks";
import {
  realGetDifficultyBreakdown,
  realGetProgressOverview,
  realGetTopicProgress,
} from "./real-progress";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

// Only the interview summary is left: its endpoint (F5 #13) exists in the DD but `algoprep-ai-review` has no
// controller yet. The blocks show the generic error; no number is invented (owner, 2026-11-07).
function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/F5_ai-review.md #13 (interview summary) is not built yet",
  );
}

export const useProgressOverview = () =>
  useQuery({
    queryKey: ["progress", "overview"],
    queryFn: () =>
      withMockData(fakeGetProgressOverview, realGetProgressOverview),
    ...QUERY_OPTIONS,
  });

export const useTopicProgress = (range: ProgressRange) =>
  useQuery({
    queryKey: ["progress", "topics", range],
    queryFn: () =>
      withMockData(
        () => fakeGetTopicProgress(range),
        () => realGetTopicProgress(range),
      ),
    ...QUERY_OPTIONS,
  });

export const useDifficultyBreakdown = () =>
  useQuery({
    queryKey: ["progress", "difficulty"],
    queryFn: () =>
      withMockData(fakeGetDifficultyBreakdown, realGetDifficultyBreakdown),
    ...QUERY_OPTIONS,
  });

/** `range` filters the whole summary (count, average, weakest, recent sessions); default "all". */
export const useInterviewSummary = (range: ProgressRange = "all") =>
  useQuery({
    queryKey: ["progress", "interview-summary", range],
    queryFn: () =>
      withMockData(() => fakeGetInterviewSummary(range), notImplemented),
    ...QUERY_OPTIONS,
  });
