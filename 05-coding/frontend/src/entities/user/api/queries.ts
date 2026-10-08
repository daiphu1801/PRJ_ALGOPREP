// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import {
  fakeGetInterviewPreferences,
  fakeGetMyProfile,
  fakeGetMySettings,
} from "./__mock__/fake-user";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

function notImplemented(): never {
  throw new Error(
    "03-dd/api/identity.md endpoints 9/14 have no real HTTP client wired up yet",
  );
}

export const useMyProfile = () =>
  useQuery({
    queryKey: ["user", "my-profile"],
    queryFn: () => withMockData(fakeGetMyProfile, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useMySettings = () =>
  useQuery({
    queryKey: ["user", "my-settings"],
    queryFn: () => withMockData(fakeGetMySettings, notImplemented),
    ...QUERY_OPTIONS,
  });

/** ai-review-owned, fetched separately per BR-07 — see entities/user/model/types.ts InterviewPracticePreferences. */
export const useInterviewPreferences = () =>
  useQuery({
    queryKey: ["user", "interview-preferences"],
    queryFn: () => withMockData(fakeGetInterviewPreferences, notImplemented),
    ...QUERY_OPTIONS,
  });
