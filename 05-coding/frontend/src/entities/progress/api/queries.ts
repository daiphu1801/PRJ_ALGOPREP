// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import type { ProgressRange } from "../model/types";
import {
  fakeGetDailySubmissions,
  fakeGetDifficultyBreakdown,
  fakeGetInterviewSummary,
  fakeGetProgressOverview,
  fakeGetTopicProgress,
} from "./__mock__/progress-mocks";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

function notImplemented(): never {
  throw new Error("03-dd/api/identity.md / api/ai-review.md don't define these endpoints yet");
}

export const useProgressOverview = () =>
  useQuery({
    queryKey: ["progress", "overview"],
    queryFn: () => withMockData(fakeGetProgressOverview, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useTopicProgress = (range: ProgressRange) =>
  useQuery({
    queryKey: ["progress", "topics", range],
    queryFn: () => withMockData(() => fakeGetTopicProgress(range), notImplemented),
    ...QUERY_OPTIONS,
  });

export const useDailySubmissions = (range: ProgressRange) =>
  useQuery({
    queryKey: ["progress", "daily-submissions", range],
    queryFn: () => withMockData(() => fakeGetDailySubmissions(range), notImplemented),
    ...QUERY_OPTIONS,
  });

export const useDifficultyBreakdown = () =>
  useQuery({
    queryKey: ["progress", "difficulty"],
    queryFn: () => withMockData(fakeGetDifficultyBreakdown, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useInterviewSummary = () =>
  useQuery({
    queryKey: ["progress", "interview-summary"],
    queryFn: () => withMockData(fakeGetInterviewSummary, notImplemented),
    ...QUERY_OPTIONS,
  });
