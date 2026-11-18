// Real calls to the identity API (03-dd/api/F1_identity.md). `mutations.ts` picks these when
// NEXT_PUBLIC_MOCK_DATA is off. Known business failures become field errors or a variant of the
// outcome; everything else is rethrown as `ApiError` and the caller shows it through `apiErrors`.
import { api, ApiError, tokenStore } from "@/shared/api";
import { setMustChangePassword } from "../model/must-change-password";
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
  Role,
  SignupOutcome,
} from "../model/types";

const BASE = "/identity/auth";

type LoginResponse = {
  accessToken: string;
  user: { baseCategory: Role; mustChangePassword?: boolean };
};

/** Runs `call`; hands a matching `ApiError` to `handle`, which may turn it into an outcome. */
async function mapKnown<T, R>(
  call: () => Promise<T>,
  onSuccess: (value: T) => R,
  handle: (error: ApiError) => R | undefined,
): Promise<R> {
  try {
    return onSuccess(await call());
  } catch (error) {
    if (error instanceof ApiError) {
      const outcome = handle(error);
      if (outcome !== undefined) return outcome;
    }
    throw error;
  }
}

export type OAuthProviderName = "google" | "github";

/** `GET /auth/oauth/{provider}/redirect` (#5): the provider URL that carries the one-time `state`. */
export async function realOAuthStart(
  provider: OAuthProviderName,
): Promise<string> {
  const response = await api.get<{ authorizationUrl: string }>(
    `${BASE}/oauth/${provider}/redirect`,
  );
  return response.authorizationUrl;
}

/**
 * `GET /auth/oauth/{provider}/callback` (#6) answers like a password login. The restore ticket of
 * a self-deactivated account (IDT-104) becomes the `deactivated` outcome and stays in memory; every
 * other failure (IDT-109, IDT-110, IDT-505) is rethrown for the caller to show through `apiErrors`.
 */
export const realOAuthCallback = (
  provider: OAuthProviderName,
  code: string,
  state: string,
): Promise<AuthOutcome> =>
  mapKnown(
    () =>
      // `api.get` takes the query object itself, not an axios config.
      api.get<LoginResponse>(`${BASE}/oauth/${provider}/callback`, {
        code,
        state,
      }),
    (response): AuthOutcome => {
      tokenStore.set(response.accessToken);
      setMustChangePassword(response.user.mustChangePassword === true);
      return { ok: true, role: response.user.baseCategory };
    },
    (error): AuthOutcome | undefined => {
      const ticket =
        error.code === "IDT-104" ? error.details?.restoreTicket : undefined;
      return typeof ticket === "string"
        ? { ok: true, deactivated: true, restoreTicket: ticket }
        : undefined;
    },
  );

export const realSignup = (input: SignupInput): Promise<SignupOutcome> =>
  mapKnown<unknown, SignupOutcome>(
    () =>
      api.post(`${BASE}/register`, {
        email: input.email,
        password: input.password,
        displayName: input.displayName,
      }),
    () => ({ ok: true, email: input.email }),
    (error) =>
      error.code === "IDT-101"
        ? { ok: false, fieldErrors: { email: "errors.emailTaken" } }
        : undefined,
  );

export const realLogin = (
  input: LoginInput,
  instructorPortal: boolean,
): Promise<AuthOutcome> =>
  mapKnown(
    () =>
      api.post<LoginResponse>(
        instructorPortal ? `${BASE}/instructor/login` : `${BASE}/login`,
        { email: input.email, password: input.password },
      ),
    (response): AuthOutcome => {
      tokenStore.set(response.accessToken);
      setMustChangePassword(response.user.mustChangePassword === true);
      return { ok: true, role: response.user.baseCategory };
    },
    (error): AuthOutcome | undefined => {
      switch (error.code) {
        case "IDT-102":
          return {
            ok: false,
            fieldErrors: { password: "errors.credentialsInvalid" },
          };
        case "IDT-111":
          return {
            ok: false,
            fieldErrors: { email: "errors.notInstructorAccount" },
          };
        case "IDT-113":
          return {
            ok: false,
            fieldErrors: { email: "errors.emailNotVerified" },
            needsEmailVerification: true,
          };
        case "IDT-104": {
          const ticket = error.details?.restoreTicket;
          return typeof ticket === "string"
            ? { ok: true, deactivated: true, restoreTicket: ticket }
            : undefined;
        }
        default:
          return undefined;
      }
    },
  );

const otpOutcome = (
  call: () => Promise<unknown>,
  invalidCode: string,
): Promise<ForgotOtpOutcome> =>
  mapKnown<unknown, ForgotOtpOutcome>(
    call,
    () => ({ ok: true }),
    (error) =>
      error.code === invalidCode
        ? { ok: false, fieldErrors: { otp: "errors.otpInvalid" } }
        : undefined,
  );

export const realVerifyEmail = (
  email: string,
  input: ForgotOtpInput,
): Promise<ForgotOtpOutcome> =>
  otpOutcome(
    () => api.post(`${BASE}/email/verify`, { email, otp: input.otp }),
    "IDT-112",
  );

export const realResendVerification = (email: string): Promise<{ ok: true }> =>
  api.post(`${BASE}/email/verify/resend`, { email }).then(() => ({ ok: true }));

export const realForgotEmail = (
  input: ForgotEmailInput,
): Promise<ForgotEmailOutcome> =>
  api
    .post(`${BASE}/password/forgot`, { email: input.email })
    .then(() => ({ ok: true }));

export const realVerifyResetOtp = (
  email: string,
  input: ForgotOtpInput,
): Promise<ForgotOtpOutcome> =>
  otpOutcome(
    () => api.post(`${BASE}/password/verify-otp`, { email, otp: input.otp }),
    "IDT-107",
  );

export const realResetPassword = (
  input: ForgotResetInput,
  email: string,
  otp: string,
): Promise<ForgotResetOutcome> =>
  api
    .post(`${BASE}/password/reset`, {
      email,
      otp,
      newPassword: input.newPassword,
    })
    .then(() => ({ ok: true }));

export const realRestoreAccount = (ticket: string): Promise<{ ok: true }> =>
  api
    .post(`${BASE}/deactivation/restore`, { restoreTicket: ticket })
    .then(() => ({ ok: true }));

export const realLogout = async (): Promise<void> => {
  const generation = tokenStore.generation();
  try {
    await api.post(`${BASE}/logout`);
  } finally {
    if (generation === tokenStore.generation()) {
      tokenStore.set(null);
      setMustChangePassword(false);
    }
  }
};
