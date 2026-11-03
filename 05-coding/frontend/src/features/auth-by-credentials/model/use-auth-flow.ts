// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { ZodError } from "zod";
import { HOME_PATH_BY_ROLE } from "@/entities/user";
import { ApiError } from "@/shared/api";
import { isSafeRedirect } from "@/shared/lib";
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
  resendVerification,
  resetPassword,
  signup,
  signupSchema,
  verifyEmail,
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
/** Any non-empty string the mock does not treat as a wrong password. */
const QUICK_LOGIN_PASSWORD = "demo1234";
/** The backend allows five wrong codes before locking a code (DD F1 #54, #60). */
const OTP_MAX_ATTEMPTS = 5;

type FieldValues = Record<string, string | boolean>;

/**
 * What the form should tell the user, as an i18n key relative to the "auth" namespace. The hook has
 * no translator (it stays testable without a provider), so `AuthForm` translates `key` and raises
 * the toast. A fresh object per event, so repeating the same failure still toasts again.
 */
export type AuthFeedback =
  | { tone: "success" | "error"; key: string; error?: undefined }
  // An API failure: the form translates it through the `apiErrors` namespace by its error code.
  | { tone: "error"; error: unknown; key?: undefined };

/** Field to mark red for the backend validation codes (the toast carries the words). */
const FIELD_BY_CODE: Record<string, string> = {
  "IDT-400": "email",
  "IDT-V19": "displayName",
  "IDT-V20": "displayName",
};
const PASSWORD_CODES = new Set(["IDT-V01", "IDT-V02", "IDT-V03", "IDT-V04"]);

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
  const [verifyTargetEmail, setVerifyTargetEmail] = useState("");
  const [otpAttemptsLeft, setOtpAttemptsLeft] = useState(OTP_MAX_ATTEMPTS);
  const [otpCooldownUntil, setOtpCooldownUntil] = useState(0);
  // One id per forgot-password journey so the mock's per-attempt OTP counter (module-level Map,
  // see entities/auth/api/__mock__/fake-auth.ts) doesn't leak across separate attempts.
  const otpSessionKey = useRef(0);
  // Kept in memory only, never in the URL or storage: the restore ticket (valid 5 minutes) and the
  // reset code that step 2 hands to step 3.
  const restoreTicket = useRef<string | undefined>(undefined);
  const resetOtp = useRef("");

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

  /** An API failure outside the cases a mutation already turns into field errors. */
  const failWith = useCallback((error: unknown, atMode: AuthMode) => {
    if (error instanceof ApiError) {
      let field = FIELD_BY_CODE[error.code];
      if (!field && PASSWORD_CODES.has(error.code)) {
        field = atMode === "forgot_reset" ? "newPassword" : "password";
      }
      if (field) setFieldErrors({ [field]: error.code });
    }
    setFeedback({ tone: "error", error });
  }, []);

  const startOtpWindow = useCallback(
    (email: string, kind: "verify" | "forgot") => {
      otpSessionKey.current += 1;
      if (kind === "verify") setVerifyTargetEmail(email);
      else setForgotTargetEmail(email);
      setOtpAttemptsLeft(OTP_MAX_ATTEMPTS);
      setOtpCooldownUntil(Date.now() + OTP_RESEND_COOLDOWN_MS);
    },
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
        isSafeRedirect(redirectTo) ? redirectTo : HOME_PATH_BY_ROLE[role],
      );
    },
    [router, searchParams],
  );

  const submitSignup = useCallback(async () => {
    const parsed = signupSchema.safeParse({
      displayName: fields.displayName ?? "",
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
      // The account exists but cannot sign in until the mailed code is confirmed (IDT-113).
      startOtpWindow(outcome.email, "verify");
      setMode("verify_email");
      confirm("notice.codeSent");
    } catch (error) {
      failWith(error, "signup");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, raise, confirm, setMode, startOtpWindow, failWith]);

  const submitVerifyEmail = useCallback(async () => {
    const parsed = forgotOtpSchema.safeParse({ otp: fields.otp ?? "" });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await verifyEmail(
        verifyTargetEmail,
        parsed.data,
        String(otpSessionKey.current),
      );
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        setOtpAttemptsLeft(
          outcome.attemptsLeft ?? Math.max(0, otpAttemptsLeft - 1),
        );
        return;
      }
      // Verifying does not sign in; send the person to login with the e-mail already filled.
      setMode("login");
      updateField("email", verifyTargetEmail);
      confirm("notice.emailVerified");
    } catch (error) {
      failWith(error, "verify_email");
    } finally {
      setIsSubmitting(false);
    }
  }, [
    fields,
    verifyTargetEmail,
    otpAttemptsLeft,
    raise,
    confirm,
    setMode,
    updateField,
    failWith,
  ]);

  /** Everything after the input is valid. Split out so `quickLogin` can reach it without going
      through the form fields — the two differ only in where the credentials come from. */
  const runLogin = useCallback(
    async (input: LoginInput) => {
      setIsSubmitting(true);
      try {
        const outcome = await loginFn(input);
        if (!outcome.ok) {
          raise(outcome.fieldErrors);
          if (outcome.needsEmailVerification) {
            // Registered but never confirmed: continue at the code step. No code cooldown, the
            // person may have lost the first mail and should be able to ask for another at once.
            startOtpWindow(input.email, "verify");
            setOtpCooldownUntil(0);
            setMode("verify_email");
          }
          return;
        }
        if (outcome.deactivated) {
          // Stays on `login` — deactivated-recovery is a variant, not a new mode
          // (02-bd/screens/shared/SHR0101_auth.md Sheet 3).
          restoreTicket.current = outcome.restoreTicket;
          setDeactivatedBanner(true);
          return;
        }
        await runLoadingOverlayThenNavigate(outcome.role);
      } catch (error) {
        failWith(error, "login");
      } finally {
        setIsSubmitting(false);
        setLoadingStep(null);
      }
    },
    [
      loginFn,
      raise,
      setMode,
      startOtpWindow,
      runLoadingOverlayThenNavigate,
      failWith,
    ],
  );

  const submitLogin = useCallback(async () => {
    const parsed = loginSchema.safeParse({
      email: fields.email ?? "",
      password: fields.password ?? "",
    });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    await runLogin(parsed.data);
  }, [fields, raise, runLogin]);

  /**
   * One-click login with a canned account, for the mock-data quick-login panel. It does NOT set
   * the fields and then submit: `submit` reads `fields` through a closure, so a value written in
   * the same tick would not be visible to it. The credentials go straight to `runLogin`, and the
   * inputs are filled only so the screen shows what was used.
   *
   * The e-mails are the ones the mock recognises by the part before "@"
   * (`entities/auth/api/__mock__/fake-auth.ts`: `admin` is ADMIN, `instructor` is INSTRUCTOR,
   * anything else is a STUDENT); the password is any string except the literal `wrong`.
   */
  const quickLogin = useCallback(
    (email: string) => {
      setModeState("login");
      setFieldErrors({});
      setDeactivatedBanner(false);
      setFields({ email, password: QUICK_LOGIN_PASSWORD });
      return runLogin({ email, password: QUICK_LOGIN_PASSWORD });
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
      } catch (error) {
        failWith(error, "login");
      } finally {
        setIsSubmitting(false);
        setLoadingStep(null);
      }
    },
    [runLoadingOverlayThenNavigate, failWith],
  );

  const submitCancelDeactivation = useCallback(async () => {
    setIsSubmitting(true);
    try {
      await cancelDeactivation(restoreTicket.current);
      restoreTicket.current = undefined;
      setDeactivatedBanner(false);
      confirm("notice.deactivationCancelled");
    } catch (error) {
      // The ticket is single-use and lasts 5 minutes: after a failure it cannot be reused, so the
      // banner goes away and the person signs in again to get a fresh one.
      restoreTicket.current = undefined;
      setDeactivatedBanner(false);
      failWith(error, "login");
    } finally {
      setIsSubmitting(false);
    }
  }, [confirm, failWith]);

  const submitForgotEmail = useCallback(async () => {
    const parsed = forgotEmailSchema.safeParse({ email: fields.email ?? "" });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      await forgotEmail(parsed.data);
      startOtpWindow(parsed.data.email, "forgot");
      setMode("forgot_otp");
      confirm("notice.codeSent");
    } catch (error) {
      failWith(error, "forgot_email");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, raise, confirm, setMode, startOtpWindow, failWith]);

  const submitForgotOtp = useCallback(async () => {
    const parsed = forgotOtpSchema.safeParse({ otp: fields.otp ?? "" });
    if (!parsed.success) {
      raise(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await verifyOtp(
        forgotTargetEmail,
        parsed.data,
        String(otpSessionKey.current),
      );
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        setOtpAttemptsLeft(
          outcome.attemptsLeft ?? Math.max(0, otpAttemptsLeft - 1),
        );
        return;
      }
      resetOtp.current = parsed.data.otp;
      setMode("forgot_reset");
    } catch (error) {
      failWith(error, "forgot_otp");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, forgotTargetEmail, otpAttemptsLeft, raise, setMode, failWith]);

  const submitResendOtp = useCallback(async () => {
    if (Date.now() < otpCooldownUntil) return;
    setIsSubmitting(true);
    try {
      if (mode === "verify_email") {
        await resendVerification(verifyTargetEmail);
        startOtpWindow(verifyTargetEmail, "verify");
      } else {
        await resendOtp(forgotTargetEmail);
        startOtpWindow(forgotTargetEmail, "forgot");
      }
      confirm("notice.codeResent");
    } catch (error) {
      failWith(error, mode);
    } finally {
      setIsSubmitting(false);
    }
  }, [
    mode,
    otpCooldownUntil,
    verifyTargetEmail,
    forgotTargetEmail,
    startOtpWindow,
    confirm,
    failWith,
  ]);

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
      const outcome = await resetPassword(
        parsed.data,
        forgotTargetEmail,
        resetOtp.current,
      );
      if (!outcome.ok) {
        raise(outcome.fieldErrors);
        return;
      }
      // Does not auto-login (01-rd/screens/shared/auth.md:58-59).
      resetOtp.current = "";
      setMode("login");
      updateField("email", forgotTargetEmail);
      confirm("notice.passwordReset");
    } catch (error) {
      failWith(error, "forgot_reset");
    } finally {
      setIsSubmitting(false);
    }
  }, [
    fields,
    forgotTargetEmail,
    raise,
    confirm,
    setMode,
    updateField,
    failWith,
  ]);

  const submit = useCallback(() => {
    switch (mode) {
      case "signup":
        return submitSignup();
      case "verify_email":
        return submitVerifyEmail();
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
    submitVerifyEmail,
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
    verifyTargetEmail,
    otpAttemptsLeft,
    otpCooldownUntil,
    submit,
    quickLogin,
    submitOAuth,
    submitCancelDeactivation,
    submitResendOtp,
  };
}
