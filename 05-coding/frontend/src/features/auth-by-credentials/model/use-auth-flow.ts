// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { ZodError } from "zod";
import { HOME_PATH_BY_ROLE } from "@/entities/user";
import {
  cancelDeactivation,
  forgotEmail,
  forgotEmailSchema,
  forgotOtpSchema,
  forgotResetSchema,
  login,
  loginSchema,
  oauthLogin,
  resendOtp,
  resetPassword,
  signup,
  signupSchema,
  verifyOtp,
  type AuthFieldErrors,
  type AuthMode,
  type AuthOutcome,
  type LoginInput,
} from "@/entities/auth";

/** Number of sequential steps shown by AuthLoadingOverlay (02-bd/screens/shared/SHR0101_auth.md Sheet 5, khu vực D). */
export const LOADING_STEP_COUNT = 4;
const LOADING_STEP_DELAY_MS = 500;
const OTP_RESEND_COOLDOWN_MS = 60_000;
/** Any non-empty string the mock does not treat as a wrong password. Long enough for `loginSchema`. */
const QUICK_LOGIN_PASSWORD = "demo1234";

type FieldValues = Record<string, string | boolean>;

/**
 * What the form should tell the user, as an i18n key relative to the "auth" namespace. The hook has
 * no translator (it stays testable without a provider), so `AuthForm` translates `key` and raises
 * the toast. A fresh object per event, so repeating the same failure still toasts again.
 */
export type AuthFeedback = { tone: "success" | "error"; key: string };

function zodErrorsToFieldErrors(error: ZodError): AuthFieldErrors {
  const result: AuthFieldErrors = {};
  for (const issue of error.issues) {
    const key = issue.path.join(".") || "form";
    result[key] ??= issue.message;
  }
  return result;
}

/**
 * `loginFn` lets a caller swap in a different login mutation without duplicating this whole hook —
 * `views/instructor-auth` passes `instructorLogin` (`DEC-2026-0925-instructor-separate-login-route`),
 * everyone else keeps the default `login`. Same trick as `AuthForm`'s `showOAuth` prop: one shared
 * state machine, per-caller behavior through a parameter.
 */
export function useAuthFlow(
  initialMode: AuthMode,
  loginFn: (input: LoginInput) => Promise<AuthOutcome> = login,
) {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [mode, setModeState] = useState<AuthMode>(initialMode);
  const [fields, setFields] = useState<FieldValues>({});
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors>({});
  const [feedback, setFeedback] = useState<AuthFeedback | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingStep, setLoadingStep] = useState<number | null>(null);
  const [deactivatedBanner, setDeactivatedBanner] = useState(false);
  const [forgotTargetEmail, setForgotTargetEmail] = useState("");
  const [otpAttemptsLeft, setOtpAttemptsLeft] = useState(5);
  const [otpCooldownUntil, setOtpCooldownUntil] = useState(0);
  // One id per forgot-password journey so the mock's per-attempt OTP counter (module-level Map,
  // see entities/auth/api/__mock__/fake-auth.ts) doesn't leak across separate attempts.
  const otpSessionKey = useRef(0);

  /** Marks the failed fields and asks for one error toast carrying the first message. */
  const raise = useCallback((errors: AuthFieldErrors) => {
    setFieldErrors(errors);
    const first = Object.values(errors).find(Boolean);
    if (first) setFeedback({ tone: "error", key: first });
  }, []);

  const confirm = useCallback(
    (key: string) => setFeedback({ tone: "success", key }),
    [],
  );

  const setMode = useCallback((next: AuthMode) => {
    setModeState(next);
    setFields({});
    setFieldErrors({});
    setDeactivatedBanner(false);
  }, []);

  const updateField = useCallback((name: string, value: string | boolean) => {
    setFields((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => {
      if (!(name in prev)) return prev;
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }, []);

  const runLoadingOverlayThenNavigate = useCallback(
    async (role: keyof typeof HOME_PATH_BY_ROLE) => {
      for (let step = 0; step < LOADING_STEP_COUNT; step += 1) {
        setLoadingStep(step);
        // Sequential by design (each step must visibly finish before the next starts), not
        // parallel work — awaiting inside the loop is intentional here.
        await new Promise((resolve) =>
          setTimeout(resolve, LOADING_STEP_DELAY_MS),
        );
      }
      // A saved `redirect`/`returnTo` wins over the role-default destination
      // (02-bd/screens/shared/SHR0101_auth.md Sheet 3).
      const redirectTo =
        searchParams.get("redirect") ?? searchParams.get("returnTo");
      router.push(
        redirectTo && redirectTo.startsWith("/")
          ? redirectTo
          : HOME_PATH_BY_ROLE[role],
      );
    },
    [router, searchParams],
  );

  const submitSignup = useCallback(async () => {
    const parsed = signupSchema.safeParse({
      username: fields.username ?? "",
      password: fields.password ?? "",
      email: fields.email ?? "",
      termsAccepted: Boolean(fields.termsAccepted),
    });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await signup(parsed.data);
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        return;
      }
      await runLoadingOverlayThenNavigate(outcome.role);
    } finally {
      setIsSubmitting(false);
      setLoadingStep(null);
    }
  }, [fields, raise, runLoadingOverlayThenNavigate]);

  /** Everything after the input is valid. Split out so `quickLogin` can reach it without going
      through the form fields — the two differ only in where the credentials come from. */
  const runLogin = useCallback(
    async (input: LoginInput) => {
      setIsSubmitting(true);
      try {
        const outcome = await loginFn(input);
        if (!outcome.ok) {
          raise(outcome.fieldErrors);
          return;
        }
        if (outcome.deactivated) {
          // Stays on `login` — deactivated-recovery is a variant, not a new mode
          // (02-bd/screens/shared/SHR0101_auth.md Sheet 3).
          setDeactivatedBanner(true);
          return;
        }
        await runLoadingOverlayThenNavigate(outcome.role);
      } finally {
        setIsSubmitting(false);
        setLoadingStep(null);
      }
    },
    [loginFn, raise, runLoadingOverlayThenNavigate],
  );

  const submitLogin = useCallback(async () => {
    const parsed = loginSchema.safeParse({
      identifier: fields.identifier ?? "",
      password: fields.password ?? "",
      rememberMe: Boolean(fields.rememberMe),
    });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    await runLogin(parsed.data);
  }, [fields, raise, runLogin]);

  /**
   * One-click login with a canned identifier, for the mock-data quick-login panel. It does NOT set
   * the fields and then submit: `submit` reads `fields` through a closure, so a value written in
   * the same tick would not be visible to it. The credentials go straight to `runLogin`, and the
   * inputs are filled only so the screen shows what was used.
   *
   * The identifiers are the reserved ones the mock recognises
   * (`entities/auth/api/__mock__/fake-auth.ts`: `admin` → ADMIN, `instructor` → INSTRUCTOR, anything
   * else → STUDENT); the password is any string except the literal `wrong`.
   */
  const quickLogin = useCallback(
    (identifier: string) => {
      setModeState("login");
      setFieldErrors({});
      setDeactivatedBanner(false);
      setFields({ identifier, password: QUICK_LOGIN_PASSWORD });
      return runLogin({
        identifier,
        password: QUICK_LOGIN_PASSWORD,
        rememberMe: false,
      });
    },
    [runLogin],
  );

  const submitOAuth = useCallback(
    async (provider: "google" | "github") => {
      setIsSubmitting(true);
      try {
        const outcome = await oauthLogin(provider);
        if (outcome.ok && !outcome.deactivated) {
          await runLoadingOverlayThenNavigate(outcome.role);
        }
      } finally {
        setIsSubmitting(false);
        setLoadingStep(null);
      }
    },
    [runLoadingOverlayThenNavigate],
  );

  const submitCancelDeactivation = useCallback(async () => {
    setIsSubmitting(true);
    try {
      await cancelDeactivation();
      setDeactivatedBanner(false);
      confirm("notice.deactivationCancelled");
    } finally {
      setIsSubmitting(false);
    }
  }, [confirm]);

  const submitForgotEmail = useCallback(async () => {
    const parsed = forgotEmailSchema.safeParse({ email: fields.email ?? "" });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      await forgotEmail(parsed.data);
      otpSessionKey.current += 1;
      setForgotTargetEmail(parsed.data.email);
      setOtpAttemptsLeft(5);
      setOtpCooldownUntil(Date.now() + OTP_RESEND_COOLDOWN_MS);
      setMode("forgot_otp");
      confirm("notice.codeSent");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, raise, confirm, setMode]);

  const submitForgotOtp = useCallback(async () => {
    const parsed = forgotOtpSchema.safeParse({ otp: fields.otp ?? "" });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await verifyOtp(
        parsed.data,
        String(otpSessionKey.current),
      );
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        setOtpAttemptsLeft(outcome.attemptsLeft);
        return;
      }
      setMode("forgot_reset");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, raise, setMode]);

  const submitResendOtp = useCallback(async () => {
    if (Date.now() < otpCooldownUntil) return;
    setIsSubmitting(true);
    try {
      await resendOtp();
      otpSessionKey.current += 1;
      setOtpAttemptsLeft(5);
      setOtpCooldownUntil(Date.now() + OTP_RESEND_COOLDOWN_MS);
      confirm("notice.codeResent");
    } finally {
      setIsSubmitting(false);
    }
  }, [otpCooldownUntil, confirm]);

  const submitForgotReset = useCallback(async () => {
    const parsed = forgotResetSchema.safeParse({
      newPassword: fields.newPassword ?? "",
      confirmPassword: fields.confirmPassword ?? "",
    });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await resetPassword(parsed.data);
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        return;
      }
      // Does not auto-login (01-rd/screens/shared/auth.md:58-59).
      setMode("login");
      confirm("notice.passwordReset");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, raise, confirm, setMode]);

  const submit = useCallback(() => {
    switch (mode) {
      case "signup":
        return submitSignup();
      case "login":
        return submitLogin();
      case "forgot_email":
        return submitForgotEmail();
      case "forgot_otp":
        return submitForgotOtp();
      case "forgot_reset":
        return submitForgotReset();
      default:
        return undefined;
    }
  }, [
    mode,
    submitSignup,
    submitLogin,
    submitForgotEmail,
    submitForgotOtp,
    submitForgotReset,
  ]);

  return {
    mode,
    setMode,
    fields,
    updateField,
    fieldErrors,
    feedback,
    isSubmitting,
    loadingStep,
    deactivatedBanner,
    forgotTargetEmail,
    otpAttemptsLeft,
    otpCooldownUntil,
    submit,
    quickLogin,
    submitOAuth,
    submitCancelDeactivation,
    submitResendOtp,
  };
}
