// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// `fakeAuth()` always succeeds in the static prototype
// [SoT: 02-bd/screens/shared/auth.md:33 — "BD chốt state machine hiển thị"]. Here we go one step
// further than the static prototype (which has zero validation) so the loading overlay and
// inline-field-error UI have something real to react to; still entirely client-side, no network.
import type {
  ForgotEmailInput,
  ForgotOtpInput,
  ForgotResetInput,
  LoginInput,
  SignupInput,
} from "../../model/schema";
import type {
  AuthOutcome,
  ForgotEmailOutcome,
  ForgotOtpOutcome,
  ForgotResetOutcome,
} from "../../model/types";

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Deterministic canned identifiers so the reviewer can drive every branch of the state machine
// without a real backend. Documented here instead of hidden in conditionals scattered around.
const RESERVED_USERNAMES = new Set(["duplicate", "admin", "instructor", "deactivated"]);
const VALID_OTP = "123456";

export async function fakeSignup(input: SignupInput): Promise<AuthOutcome> {
  await delay(600);
  if (input.username.toLowerCase() === "duplicate") {
    return { ok: false, fieldErrors: { username: "errors.usernameTaken" } };
  }
  return { ok: true, role: "STUDENT" };
}

export async function fakeLogin(input: LoginInput): Promise<AuthOutcome> {
  await delay(600);
  const id = input.identifier.trim().toLowerCase();

  if (id === "deactivated") {
    // deactivated-recovery variant of `login` (02-bd/screens/shared/auth.md section 3) — the
    // credentials are otherwise correct, so this is `ok: true` with a flag, not a field error.
    return { ok: true, role: "STUDENT", deactivated: true };
  }
  if (input.password === "wrong") {
    return { ok: false, fieldErrors: { password: "errors.credentialsInvalid" } };
  }
  if (id === "admin") return { ok: true, role: "ADMIN" };
  if (id === "instructor") return { ok: true, role: "INSTRUCTOR" };
  if (RESERVED_USERNAMES.has(id) === false && id.length === 0) {
    return { ok: false, fieldErrors: { identifier: "errors.identifierRequired" } };
  }
  return { ok: true, role: "STUDENT" };
}

export async function fakeOAuthLogin(_provider: "google" | "github"): Promise<AuthOutcome> {
  // Prototype note (BD Q2, 02-bd/screens/shared/auth.md:141): both OAuth buttons call the same
  // fake success path in the static prototype — a real redirect-based flow is DD's job.
  await delay(900);
  return { ok: true, role: "STUDENT" };
}

export async function fakeCancelDeactivation(): Promise<{ ok: true }> {
  await delay(400);
  return { ok: true };
}

export async function fakeForgotEmail(_input: ForgotEmailInput): Promise<ForgotEmailOutcome> {
  // Always succeeds, even for an email that doesn't exist — do not reveal account existence
  // [SoT: 01-rd/screens/shared/auth.md:50-55].
  await delay(500);
  return { ok: true };
}

const otpAttempts = new Map<string, number>();

export async function fakeVerifyOtp(input: ForgotOtpInput, sessionKey: string): Promise<ForgotOtpOutcome> {
  await delay(500);
  if (input.otp === VALID_OTP) {
    otpAttempts.delete(sessionKey);
    return { ok: true };
  }
  const attempts = (otpAttempts.get(sessionKey) ?? 0) + 1;
  otpAttempts.set(sessionKey, attempts);
  const attemptsLeft = Math.max(0, 5 - attempts);
  return { ok: false, fieldErrors: { otp: "errors.otpInvalid" }, attemptsLeft };
}

export async function fakeResendOtp(): Promise<{ ok: true }> {
  await delay(400);
  return { ok: true };
}

export async function fakeResetPassword(_input: ForgotResetInput): Promise<ForgotResetOutcome> {
  await delay(600);
  return { ok: true };
}
