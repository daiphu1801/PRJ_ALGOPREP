// Boundary functions the feature layer calls. Each routes through withMockData: with
// NEXT_PUBLIC_MOCK_DATA on it uses the canned data in `__mock__/`, otherwise the real identity API
// in `real-auth.ts` (03-dd/api/F1_identity.md).
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
  SignupOutcome,
} from "../model/types";
import {
  fakeCancelDeactivation,
  fakeForgotEmail,
  fakeInstructorLogin,
  fakeLogin,
  fakeOAuthLogin,
  fakeResendOtp,
  fakeResetPassword,
  fakeSignup,
  fakeVerifyOtp,
} from "./__mock__/fake-auth";
import {
  type OAuthProviderName,
  realForgotEmail,
  realLogin,
  realLogout,
  realOAuthCallback,
  realOAuthStart,
  realResendVerification,
  realResetPassword,
  realRestoreAccount,
  realSignup,
  realVerifyEmail,
  realVerifyResetOtp,
} from "./real-auth";

export const signup = (input: SignupInput): Promise<SignupOutcome> =>
  withMockData(
    () => fakeSignup(input),
    () => realSignup(input),
  );

/** Confirms the code mailed after signup (`POST /auth/email/verify`, endpoint #53). */
export const verifyEmail = (
  email: string,
  input: ForgotOtpInput,
  sessionKey: string,
): Promise<ForgotOtpOutcome> =>
  withMockData(
    () => fakeVerifyOtp(input, sessionKey),
    () => realVerifyEmail(email, input),
  );

/** Mails a new signup code (`POST /auth/email/verify/resend`, endpoint #54). */
export const resendVerification = (email: string): Promise<{ ok: true }> =>
  withMockData(
    () => fakeResendOtp(),
    () => realResendVerification(email),
  );

export const login = (input: LoginInput): Promise<AuthOutcome> =>
  withMockData(
    () => fakeLogin(input),
    () => realLogin(input, false),
  );

// `POST /auth/instructor/login` (03-dd/api/F1_identity.md endpoint #49,
// `DEC-2026-0925-instructor-separate-login-route`) — dedicated endpoint for `views/instructor-auth`.
export const instructorLogin = (input: LoginInput): Promise<AuthOutcome> =>
  withMockData(
    () => fakeInstructorLogin(input),
    () => realLogin(input, true),
  );

/**
 * Mock mode signs in on the spot. Real mode leaves the app for the provider (#5); the promise never
 * settles because the page is gone, which keeps the button busy until the browser navigates. The
 * provider sends the person back to `/auth/callback/{provider}`, which calls `oauthCallback`.
 */
export const oauthLogin = (provider: OAuthProviderName): Promise<AuthOutcome> =>
  withMockData(
    () => fakeOAuthLogin(provider),
    async () => {
      window.location.assign(await realOAuthStart(provider));
      return new Promise<AuthOutcome>(() => {});
    },
  );

/** Finishes a real OAuth sign-in (#6) on the callback route. */
export const oauthCallback = (
  provider: OAuthProviderName,
  code: string,
  state: string,
): Promise<AuthOutcome> =>
  withMockData(
    () => fakeOAuthLogin(provider),
    () => realOAuthCallback(provider, code, state),
  );

/** Restores a self-deleted account with the one-use ticket from the IDT-104 login failure (#61). */
export const cancelDeactivation = (ticket?: string): Promise<{ ok: true }> =>
  withMockData(
    () => fakeCancelDeactivation(),
    () => {
      if (!ticket) {
        throw new ApiError("IDT-114", 401, "Missing restore ticket");
      }
      return realRestoreAccount(ticket);
    },
  );

export const logout = (): Promise<void> =>
  withMockData(
    () => Promise.resolve(),
    () => realLogout(),
  );

export const forgotEmail = (
  input: ForgotEmailInput,
): Promise<ForgotEmailOutcome> =>
  withMockData(
    () => fakeForgotEmail(input),
    () => realForgotEmail(input),
  );

export const verifyOtp = (
  email: string,
  input: ForgotOtpInput,
  sessionKey: string,
): Promise<ForgotOtpOutcome> =>
  withMockData(
    () => fakeVerifyOtp(input, sessionKey),
    () => realVerifyResetOtp(email, input),
  );

/** Asking for a new reset code is the same call as the first request (`POST /auth/password/forgot`). */
export const resendOtp = (email: string): Promise<{ ok: true }> =>
  withMockData(
    () => fakeResendOtp(),
    () => realForgotEmail({ email }).then(() => ({ ok: true as const })),
  );

export const resetPassword = (
  input: ForgotResetInput,
  email: string,
  otp: string,
): Promise<ForgotResetOutcome> =>
  withMockData(
    () => fakeResetPassword(input),
    () => realResetPassword(input, email, otp),
  );
