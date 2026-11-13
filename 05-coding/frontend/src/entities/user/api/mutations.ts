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
import {
  realConfirmEmailChange,
  realRequestEmailChange,
} from "./real-email-change";
import { realDeleteMyAccount } from "./real-account";
import {
  realExportInterviewTranscripts,
  type ExportedFile,
  realExportMyData,
} from "./real-export";
import { realChangeMyPassword } from "./real-password";
import { realUpdateMyProfile } from "./real-profile";
import {
  realUpdateInterviewPreferences,
  realUpdateMySettings,
} from "./real-settings";

export const updateMyProfile = (
  input: UpdateProfileInput,
): Promise<UserProfile> =>
  withMockData(
    () => fakeUpdateMyProfile(input),
    () => realUpdateMyProfile(input),
  );

export const requestEmailChange = (newEmail: string): Promise<{ ok: true }> =>
  withMockData(
    () => fakeRequestEmailChange(newEmail),
    () => realRequestEmailChange(newEmail),
  );

export const confirmEmailChange = (code: string, newEmail: string) =>
  withMockData(
    () => fakeConfirmEmailChange(code, newEmail),
    // The server remembers which address the code was sent to; only the code travels.
    () => realConfirmEmailChange(code),
  );

export const changeMyPassword = (input: ChangePasswordInput) =>
  withMockData(
    () => fakeChangeMyPassword(input),
    () => realChangeMyPassword(input),
  );

export const updateMySettings = (
  input: AccountSettings,
): Promise<AccountSettings> =>
  withMockData(
    () => fakeUpdateMySettings(input),
    () => realUpdateMySettings(input),
  );

/** ai-review-owned — called INDEPENDENTLY of updateMySettings per BR-07, never Promise.all'd into one atomic unit. */
export const updateInterviewPracticePreferences = (
  input: InterviewPracticePreferences,
): Promise<InterviewPracticePreferences> =>
  withMockData(
    () => fakeUpdateInterviewPreferences(input),
    () => realUpdateInterviewPreferences(input),
  );

export const exportMyData = (
  scope: "profile" | "submissions",
): Promise<ExportedFile> =>
  withMockData(
    async () => ({ blob: await fakeExportMyData(scope), truncated: false }),
    () => realExportMyData(scope),
  );

export const exportMyInterviewTranscripts = (): Promise<Blob> =>
  withMockData(
    () => fakeExportInterviewTranscripts(),
    () => realExportInterviewTranscripts(),
  );

export const deleteMyAccount = (emailConfirmation: string) =>
  withMockData(
    () => fakeDeleteMyAccount(emailConfirmation),
    () => realDeleteMyAccount(emailConfirmation),
  );
