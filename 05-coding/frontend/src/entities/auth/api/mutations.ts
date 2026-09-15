// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Boundary functions the feature layer calls. Each routes through withMockData so flipping
// NEXT_PUBLIC_MOCK_DATA off is the only thing graduation has to do here — `fetchReal` throws until
// 03-dd/api/identity.md exists and a real client.ts call replaces it (vibecode-pipeline SKILL.md
// Layer 3 "Graduation").
import { ApiError, withMockData } from "@/shared/api";
import type {
  ForgotEmailInput,
  ForgotOtpInput,
  ForgotResetInput,
  LoginInput,
  SignupInput,
} from "../model/schema";
import type {
  AuthOutcome,
  ForgotEmailOutcome,
  ForgotOtpOutcome,
  ForgotResetOutcome,
} from "../model/types";
import {
  fakeCancelDeactivation,
  fakeForgotEmail,
  fakeLogin,
  fakeOAuthLogin,
  fakeResendOtp,
  fakeResetPassword,
  fakeSignup,
  fakeVerifyOtp,
} from "./__mock__/fake-auth";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/identity.md does not exist yet");
}

export const signup = (input: SignupInput): Promise<AuthOutcome> =>
  withMockData(() => fakeSignup(input), notImplemented);

export const login = (input: LoginInput): Promise<AuthOutcome> =>
  withMockData(() => fakeLogin(input), notImplemented);

export const oauthLogin = (provider: "google" | "github"): Promise<AuthOutcome> =>
  withMockData(() => fakeOAuthLogin(provider), notImplemented);

export const cancelDeactivation = (): Promise<{ ok: true }> =>
  withMockData(() => fakeCancelDeactivation(), notImplemented);

export const forgotEmail = (input: ForgotEmailInput): Promise<ForgotEmailOutcome> =>
  withMockData(() => fakeForgotEmail(input), notImplemented);

export const verifyOtp = (input: ForgotOtpInput, sessionKey: string): Promise<ForgotOtpOutcome> =>
  withMockData(() => fakeVerifyOtp(input, sessionKey), notImplemented);

export const resendOtp = (): Promise<{ ok: true }> => withMockData(() => fakeResendOtp(), notImplemented);

export const resetPassword = (input: ForgotResetInput): Promise<ForgotResetOutcome> =>
  withMockData(() => fakeResetPassword(input), notImplemented);
