// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// One TanStack Query hook per block, matching BD's recommendation to keep 9 endpoints separate so
// one failing source doesn't fail the whole payload (02-bd/screens/admin/admin_overview.md
// section 5 — "khuyến nghị giữ riêng theo khối", [SoT: Suy luận]). Each hook's queryFn goes
// through withMockData so graduation only means deleting the __mock__ import here.
import { useQuery } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import {
  fakeActiveUsersSummary,
  fakeDifficultyBreakdown,
  fakeSubmissionsByDay,
  fakeSubmissionsByLanguage,
  fakeSubmissionsByMonth,
  fakeSubmissionsSummary,
  fakeTopProblems,
  fakeUserRetention,
  fakeVerdictDistribution,
} from "./__mock__/dashboard-mocks";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/identity.md does not define these endpoints yet");
}

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

export const useSubmissionsSummary = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-summary"],
    queryFn: () => withMockData(fakeSubmissionsSummary, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useActiveUsersSummary = () =>
  useQuery({
    queryKey: ["admin-dashboard", "active-users-summary"],
    queryFn: () => withMockData(fakeActiveUsersSummary, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByLanguage = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-language"],
    queryFn: () => withMockData(fakeSubmissionsByLanguage, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useVerdictDistribution = () =>
  useQuery({
    queryKey: ["admin-dashboard", "verdict-distribution"],
    queryFn: () => withMockData(fakeVerdictDistribution, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useDifficultyBreakdown = () =>
  useQuery({
    queryKey: ["admin-dashboard", "difficulty-breakdown"],
    queryFn: () => withMockData(fakeDifficultyBreakdown, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByDay = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-day"],
    queryFn: () => withMockData(fakeSubmissionsByDay, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useSubmissionsByMonth = () =>
  useQuery({
    queryKey: ["admin-dashboard", "submissions-by-month"],
    queryFn: () => withMockData(fakeSubmissionsByMonth, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useTopProblems = () =>
  useQuery({
    queryKey: ["admin-dashboard", "top-problems"],
    queryFn: () => withMockData(fakeTopProblems, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useUserRetention = () =>
  useQuery({
    queryKey: ["admin-dashboard", "user-retention"],
    queryFn: () => withMockData(fakeUserRetention, notImplemented),
    ...QUERY_OPTIONS,
  });
