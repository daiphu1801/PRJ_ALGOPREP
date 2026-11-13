// Data boundary of the AI usage screen (ADM0302): one query per block (F5 #27 to #31), so a failing
// block shows its own error and the others stay readable. Mock mode serves the local page.
import { useMockMutation, useMockQuery } from "@/shared/api";
import {
  mockDailyUsage,
  mockMarkAlertReviewed,
  mockTopProblems,
  mockTopUsers,
  mockUsageAlerts,
} from "./__mock__/ai-usage-mocks";
import {
  realGetDailyUsage,
  realListTopProblems,
  realListTopUsers,
  realListUsageAlerts,
  realMarkAlertReviewed,
} from "./real-usage";
import type { UsageRange } from "../model/types";

const SPEC = "03-dd/api/F5_ai-review.md";
export const ALERTS_KEY = ["ai-usage", "alerts"] as const;

/** #27. The key carries the range, so a late answer for an old range never lands in the new chart. */
export const useAiUsageDaily = (range: UsageRange) =>
  useMockQuery({
    queryKey: ["ai-usage", "daily", range],
    mock: () => mockDailyUsage(range),
    real: () => realGetDailyUsage(range),
    spec: SPEC,
    staleTime: 0,
    keepPrevious: true,
  });

/** #28. */
export const useAiUsageAlerts = () =>
  useMockQuery({
    queryKey: ALERTS_KEY,
    mock: mockUsageAlerts,
    real: realListUsageAlerts,
    spec: SPEC,
    staleTime: 0,
  });

/** #29: a reviewed alert leaves the list once the list is refetched. */
export const useMarkAlertReviewed = () =>
  useMockMutation<string>({
    mock: mockMarkAlertReviewed,
    real: realMarkAlertReviewed,
    spec: SPEC,
    invalidate: [ALERTS_KEY],
  });

/** #30. */
export const useAiTopUsers = () =>
  useMockQuery({
    queryKey: ["ai-usage", "top-users"],
    mock: mockTopUsers,
    real: realListTopUsers,
    spec: SPEC,
    staleTime: 0,
  });

/** #31. */
export const useAiTopProblems = () =>
  useMockQuery({
    queryKey: ["ai-usage", "top-problems"],
    mock: mockTopProblems,
    real: realListTopProblems,
    spec: SPEC,
    staleTime: 0,
  });
