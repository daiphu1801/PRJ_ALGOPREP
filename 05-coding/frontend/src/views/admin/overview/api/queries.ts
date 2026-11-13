// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// One TanStack Query hook per block, matching BD's recommendation to keep 9 endpoints separate so
// one failing source doesn't fail the whole payload (02-bd/screens/admin/ADM0101_overview.md
// section 5 — "khuyến nghị giữ riêng theo khối", [SoT: Suy luận]). Each hook's queryFn goes
// through withMockData so graduation only means deleting the __mock__ import here.
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import {
  fakeActiveUsersSummary,
  fakeDifficultyBreakdown,
  fakePasswordResetsSummary,
  fakeSubmissionsByDay,
  fakeSubmissionsByLanguage,
  fakeSubmissionsByMonth,
  fakeSubmissionsSummary,
  fakeTopProblems,
  fakeUserRetention,
  fakeVerdictDistribution,
} from "./__mock__/dashboard-mocks";
import {
  realActiveUsersSummary,
  realPasswordResetsSummary,
  realUserRetention,
} from "./real-dashboard";
import {
  realDifficultyBreakdown,
  realSubmissionsByDay,
  realSubmissionsByLanguage,
  realSubmissionsByMonth,
  realSubmissionsSummary,
  realTopProblems,
  realVerdictDistribution,
} from "./real-submissions";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

export const useSubmissionsSummary = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-summary"],
    queryFn: () => withMockData(fakeSubmissionsSummary, realSubmissionsSummary),
    ...QUERY_OPTIONS,
  });

export const useActiveUsersSummary = () =>
  useQuery({
    queryKey: ["admin-dashboard", "active-users-summary"],
    queryFn: () => withMockData(fakeActiveUsersSummary, realActiveUsersSummary),
    ...QUERY_OPTIONS,
  });

export const usePasswordResetsSummary = () =>
  useQuery({
    queryKey: ["admin-dashboard", "password-resets-summary"],
    queryFn: () =>
      withMockData(fakePasswordResetsSummary, realPasswordResetsSummary),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByLanguage = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-language"],
    queryFn: () =>
      withMockData(fakeSubmissionsByLanguage, realSubmissionsByLanguage),
    ...QUERY_OPTIONS,
  });

export const useVerdictDistribution = () =>
  useQuery({
    queryKey: ["admin-dashboard", "verdict-distribution"],
    queryFn: () =>
      withMockData(fakeVerdictDistribution, realVerdictDistribution),
    ...QUERY_OPTIONS,
  });

export const useDifficultyBreakdown = () =>
  useQuery({
    queryKey: ["admin-dashboard", "difficulty-breakdown"],
    queryFn: () =>
      withMockData(fakeDifficultyBreakdown, realDifficultyBreakdown),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByDay = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-day"],
    queryFn: () => withMockData(fakeSubmissionsByDay, realSubmissionsByDay),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByMonth = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-month"],
    queryFn: () => withMockData(fakeSubmissionsByMonth, realSubmissionsByMonth),
    ...QUERY_OPTIONS,
  });

export const useTopProblems = () =>
  useQuery({
    queryKey: ["admin-dashboard", "top-problems"],
    queryFn: () => withMockData(fakeTopProblems, realTopProblems),
    ...QUERY_OPTIONS,
  });

export const useUserRetention = () =>
  useQuery({
    queryKey: ["admin-dashboard", "user-retention"],
    queryFn: () => withMockData(fakeUserRetention, realUserRetention),
    ...QUERY_OPTIONS,
  });
