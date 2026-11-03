// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import {
  fakeGetInterviewPreferences,
  fakeGetMyProfile,
  fakeGetMySettings,
} from "./__mock__/fake-user";
import { realGetMyProfile } from "./real-profile";
import {
  realGetInterviewPreferences,
  realGetMySettings,
} from "./real-settings";

const QUERY_OPTIONS = { staleTime: 30_000, retry: false } as const;

export const useMyProfile = (
  area: "student" | "instructor" | "admin" = "student",
) =>
  useQuery({
    queryKey: ["user", "my-profile", area],
    queryFn: () =>
      withMockData(() => fakeGetMyProfile(area), realGetMyProfile),
    ...QUERY_OPTIONS,
  });

export const useMySettings = () =>
  useQuery({
    queryKey: ["user", "my-settings"],
    queryFn: () => withMockData(fakeGetMySettings, realGetMySettings),
    ...QUERY_OPTIONS,
  });

/** ai-review-owned, fetched separately per BR-07 — see entities/user/model/types.ts InterviewPracticePreferences. */
export const useInterviewPreferences = () =>
  useQuery({
    queryKey: ["user", "interview-preferences"],
    queryFn: () =>
      withMockData(fakeGetInterviewPreferences, realGetInterviewPreferences),
    ...QUERY_OPTIONS,
  });
