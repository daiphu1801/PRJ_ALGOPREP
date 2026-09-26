// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Plain async functions (not useMutation hooks) — matches entities/auth/api/mutations.ts's
// convention, letting features own their own pending/error state via useState instead of coupling
// to TanStack Query's mutation cache for what is, at prototype stage, a single in-flight call.
import { withMockData } from "@/shared/api";
import type { ChangePasswordInput } from "../model/schema";
import type {
  AccountSettings,
  InterviewPracticePreferences,
  UpdateProfileInput,
  UserProfile,
} from "../model/types";
import {
  fakeChangeMyPassword,
  fakeConfirmEmailChange,
  fakeDeleteMyAccount,
  fakeExportInterviewTranscripts,
  fakeExportMyData,
  fakeRequestEmailChange,
  fakeUpdateInterviewPreferences,
  fakeUpdateMyProfile,
  fakeUpdateMySettings,
} from "./__mock__/fake-user";

function notImplemented(): never {
  throw new Error("03-dd/api/identity.md write endpoints have no real HTTP client wired up yet");
}

export const updateMyProfile = (input: UpdateProfileInput): Promise<UserProfile> =>
  withMockData(() => fakeUpdateMyProfile(input), notImplemented);

export const requestEmailChange = (newEmail: string): Promise<{ ok: true }> =>
  withMockData(() => fakeRequestEmailChange(newEmail), notImplemented);

export const confirmEmailChange = (code: string, newEmail: string) =>
  withMockData(() => fakeConfirmEmailChange(code, newEmail), notImplemented);

export const changeMyPassword = (input: ChangePasswordInput) =>
  withMockData(() => fakeChangeMyPassword(input), notImplemented);

export const updateMySettings = (input: AccountSettings): Promise<AccountSettings> =>
  withMockData(() => fakeUpdateMySettings(input), notImplemented);

/** ai-review-owned — called INDEPENDENTLY of updateMySettings per BR-07, never Promise.all'd into one atomic unit. */
export const updateInterviewPracticePreferences = (
  input: InterviewPracticePreferences,
): Promise<InterviewPracticePreferences> =>
  withMockData(() => fakeUpdateInterviewPreferences(input), notImplemented);

export const exportMyData = (scope: "profile" | "submissions"): Promise<Blob> =>
  withMockData(() => fakeExportMyData(scope), notImplemented);

export const exportMyInterviewTranscripts = (): Promise<Blob> =>
  withMockData(() => fakeExportInterviewTranscripts(), notImplemented);

export const deleteMyAccount = (emailConfirmation: string) =>
  withMockData(() => fakeDeleteMyAccount(emailConfirmation), notImplemented);
