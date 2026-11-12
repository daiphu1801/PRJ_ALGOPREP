// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
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
import { realGetInterviewSummary } from "./real-interview-summary";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

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
      withMockData(
        () => fakeGetInterviewSummary(range),
        () => realGetInterviewSummary(range),
      ),
    ...QUERY_OPTIONS,
  });
