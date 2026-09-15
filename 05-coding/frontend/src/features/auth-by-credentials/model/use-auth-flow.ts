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
} from "@/entities/auth";

/** Number of sequential steps shown by AuthLoadingOverlay (02-bd/screens/shared/auth.md section 1). */
export const LOADING_STEP_COUNT = 4;
const LOADING_STEP_DELAY_MS = 500;
const OTP_RESEND_COOLDOWN_MS = 60_000;

type FieldValues = Record<string, string | boolean>;

function zodErrorsToFieldErrors(error: ZodError): AuthFieldErrors {
  const result: AuthFieldErrors = {};
  for (const issue of error.issues) {
    const key = issue.path.join(".") || "form";
    result[key] ??= issue.message;
  }
  return result;
}

export function useAuthFlow(initialMode: AuthMode) {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [mode, setModeState] = useState<AuthMode>(initialMode);
  const [fields, setFields] = useState<FieldValues>({});
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingStep, setLoadingStep] = useState<number | null>(null);
  const [deactivatedBanner, setDeactivatedBanner] = useState(false);
  const [forgotTargetEmail, setForgotTargetEmail] = useState("");
  const [otpAttemptsLeft, setOtpAttemptsLeft] = useState(5);
  const [otpCooldownUntil, setOtpCooldownUntil] = useState(0);
  // One id per forgot-password journey so the mock's per-attempt OTP counter (module-level Map,
  // see entities/auth/api/__mock__/fake-auth.ts) doesn't leak across separate attempts.
  const otpSessionKey = useRef(0);

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
        await new Promise((resolve) => setTimeout(resolve, LOADING_STEP_DELAY_MS));
      }
      // A saved `redirect`/`returnTo` wins over the role-default destination
      // (02-bd/screens/shared/auth.md section 5).
      const redirectTo = searchParams.get("redirect") ?? searchParams.get("returnTo");
      router.push(redirectTo && redirectTo.startsWith("/") ? redirectTo : HOME_PATH_BY_ROLE[role]);
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
      setFieldErrors(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await signup(parsed.data);
      if (!outcome.ok) {
        setFieldErrors(outcome.fieldErrors);
        return;
      }
      await runLoadingOverlayThenNavigate(outcome.role);
    } finally {
      setIsSubmitting(false);
      setLoadingStep(null);
    }
  }, [fields, runLoadingOverlayThenNavigate]);

  const submitLogin = useCallback(async () => {
    const parsed = loginSchema.safeParse({
      identifier: fields.identifier ?? "",
      password: fields.password ?? "",
      rememberMe: Boolean(fields.rememberMe),
    });
    if (!parsed.success) {
      setFieldErrors(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await login(parsed.data);
      if (!outcome.ok) {
        setFieldErrors(outcome.fieldErrors);
        return;
      }
      if (outcome.deactivated) {
        // Stays on `login` — deactivated-recovery is a variant, not a new mode
        // (02-bd/screens/shared/auth.md section 3).
        setDeactivatedBanner(true);
        return;
      }
      await runLoadingOverlayThenNavigate(outcome.role);
    } finally {
      setIsSubmitting(false);
      setLoadingStep(null);
    }
  }, [fields, runLoadingOverlayThenNavigate]);

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
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const submitForgotEmail = useCallback(async () => {
    const parsed = forgotEmailSchema.safeParse({ email: fields.email ?? "" });
    if (!parsed.success) {
      setFieldErrors(zodErrorsToFieldErrors(parsed.error));
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
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, setMode]);

  const submitForgotOtp = useCallback(async () => {
    const parsed = forgotOtpSchema.safeParse({ otp: fields.otp ?? "" });
    if (!parsed.success) {
      setFieldErrors(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await verifyOtp(parsed.data, String(otpSessionKey.current));
      if (!outcome.ok) {
        setFieldErrors(outcome.fieldErrors);
        setOtpAttemptsLeft(outcome.attemptsLeft);
        return;
      }
      setMode("forgot_reset");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, setMode]);

  const submitResendOtp = useCallback(async () => {
    if (Date.now() < otpCooldownUntil) return;
    setIsSubmitting(true);
    try {
      await resendOtp();
      otpSessionKey.current += 1;
      setOtpAttemptsLeft(5);
      setOtpCooldownUntil(Date.now() + OTP_RESEND_COOLDOWN_MS);
    } finally {
      setIsSubmitting(false);
    }
  }, [otpCooldownUntil]);

  const submitForgotReset = useCallback(async () => {
    const parsed = forgotResetSchema.safeParse({
      newPassword: fields.newPassword ?? "",
      confirmPassword: fields.confirmPassword ?? "",
    });
    if (!parsed.success) {
      setFieldErrors(zodErrorsToFieldErrors(parsed.error));
      return;
    }
    setIsSubmitting(true);
    try {
      const outcome = await resetPassword(parsed.data);
      if (!outcome.ok) {
        setFieldErrors(outcome.fieldErrors);
        return;
      }
      // Does not auto-login (01-rd/screens/shared/auth.md:58-59).
      setMode("login");
    } finally {
      setIsSubmitting(false);
    }
  }, [fields, setMode]);

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
  }, [mode, submitSignup, submitLogin, submitForgotEmail, submitForgotOtp, submitForgotReset]);

  return {
    mode,
    setMode,
    fields,
    updateField,
    fieldErrors,
    isSubmitting,
    loadingStep,
    deactivatedBanner,
    forgotTargetEmail,
    otpAttemptsLeft,
    otpCooldownUntil,
    submit,
    submitOAuth,
    submitCancelDeactivation,
    submitResendOtp,
  };
}
