// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useState, type FormEvent } from "react";
import { useT } from "@/shared/i18n";
import { Button, InlineFieldError } from "@/shared/ui";
import type { useAuthFlow } from "../model/use-auth-flow";
import { OAuthButtonGroup } from "./oauth-button-group";
import { OtpInputGroup } from "./otp-input-group";

type AuthFormProps = {
  flow: ReturnType<typeof useAuthFlow>;
  /**
   * Admin login (`views/admin-auth`, `DEC-2026-0915-admin-separate-login-route`) hides the OAuth
   * row — admin accounts are provisioned, not self-service via Google/GitHub — without needing a
   * second copy of this form. Defaults to true so the shared `auth` screen (student/instructor/
   * public) keeps rendering it exactly as before.
   */
  showOAuth?: boolean;
};

/**
 * Dynamic field list per mode (`DynamicFieldList`, 02-bd/screens/shared/auth.md section 2) plus
 * every mode-specific control (password reveal, terms checkbox, remember-me, forgot-password link,
 * OAuth group, deactivated-account banner). Kept as one component instead of five tiny ones —
 * the fields ARE the one user action (`auth-by-credentials`), splitting further would scatter one
 * action's state across files for no reuse benefit (frontend_architecture.md 2.A: "gắn với một
 * hành động của người dùng" stays in features/<action>/ui as a unit).
 */
export function AuthForm({ flow, showOAuth = true }: AuthFormProps) {
  const t = useT("auth");
  const [showPassword, setShowPassword] = useState(false);

  const {
    mode,
    fields,
    updateField,
    fieldErrors,
    isSubmitting,
    deactivatedBanner,
    forgotTargetEmail,
    otpAttemptsLeft,
    otpCooldownUntil,
    submit,
    submitOAuth,
    submitCancelDeactivation,
    submitResendOtp,
    setMode,
  } = flow;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    void submit();
  };

  // fieldErrors stores i18n KEYS relative to the "auth" namespace (see entities/auth/model/schema.ts),
  // not display text — translate here so the mock/zod layer never has to know about locales.
  const fieldErrorMessage = (name: string): string | undefined =>
    fieldErrors[name] ? t(fieldErrors[name]) : undefined;

  const textField = (name: string, label: string, type: "text" | "email" = "text") => (
    <div>
      <label htmlFor={name} className="mb-1 block text-sm font-medium text-[var(--color-text)]">
        {label}
      </label>
      <input
        id={name}
        type={type}
        value={typeof fields[name] === "string" ? fields[name] : ""}
        disabled={isSubmitting}
        onChange={(e) => updateField(name, e.target.value)}
        className="w-full rounded-md border border-[var(--color-border)] bg-transparent px-3 py-2 text-sm text-[var(--color-text)]"
        aria-describedby={`${name}-error`}
      />
      <div id={`${name}-error`}>
        <InlineFieldError message={fieldErrorMessage(name)} />
      </div>
    </div>
  );

  const passwordField = (name: string, label: string) => (
    <div>
      <label htmlFor={name} className="mb-1 block text-sm font-medium text-[var(--color-text)]">
        {label}
      </label>
      <div className="relative">
        <input
          id={name}
          type={showPassword ? "text" : "password"}
          value={typeof fields[name] === "string" ? fields[name] : ""}
          disabled={isSubmitting}
          onChange={(e) => updateField(name, e.target.value)}
          className="w-full rounded-md border border-[var(--color-border)] bg-transparent px-3 py-2 pr-16 text-sm text-[var(--color-text)]"
          aria-describedby={`${name}-error`}
        />
        <button
          type="button"
          onClick={() => setShowPassword((prev) => !prev)}
          className="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-[var(--color-primary)]"
        >
          {showPassword ? t("passwordHide") : t("passwordShow")}
        </button>
      </div>
      <div id={`${name}-error`}>
        <InlineFieldError message={fieldErrorMessage(name)} />
      </div>
    </div>
  );

  if (mode === "forgot_email") {
    return (
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <p className="text-sm text-[var(--color-text-muted)]">{t("forgotEmailHint")}</p>
        {textField("email", t("emailLabel"), "email")}
        <Button type="submit" disabled={isSubmitting}>
          {t("forgotEmailSubmit")}
        </Button>
        <Button type="button" variant="ghost" onClick={() => setMode("login")}>
          {t("backToLogin")}
        </Button>
      </form>
    );
  }

  if (mode === "forgot_otp") {
    return (
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <p className="text-sm text-[var(--color-text-muted)]">{t("forgotOtpHint", { email: forgotTargetEmail })}</p>
        <OtpInputGroup
          value={typeof fields.otp === "string" ? fields.otp : ""}
          onChange={(value) => updateField("otp", value)}
          error={fieldErrorMessage("otp")}
          attemptsLeft={otpAttemptsLeft}
          cooldownUntil={otpCooldownUntil}
          onResend={() => void submitResendOtp()}
          disabled={isSubmitting}
        />
        <Button type="submit" disabled={isSubmitting}>
          {t("forgotOtpSubmit")}
        </Button>
      </form>
    );
  }

  if (mode === "forgot_reset") {
    return (
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {passwordField("newPassword", t("newPasswordLabel"))}
        {passwordField("confirmPassword", t("confirmPasswordLabel"))}
        <Button type="submit" disabled={isSubmitting}>
          {t("forgotResetSubmit")}
        </Button>
      </form>
    );
  }

  const isSignup = mode === "signup";

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {deactivatedBanner && (
        <div role="alert" className="rounded-md border border-[var(--color-danger)] p-3 text-sm text-[var(--color-danger)]">
          <p>{t("deactivatedBannerMessage")}</p>
          <Button type="button" size="sm" variant="ghost" className="mt-2" onClick={() => void submitCancelDeactivation()}>
            {t("deactivatedBannerCancel")}
          </Button>
        </div>
      )}

      {isSignup ? textField("username", t("usernameLabel")) : textField("identifier", t("identifierLabel"))}
      {passwordField("password", t("passwordLabel"))}
      {isSignup && textField("email", t("emailLabel"), "email")}

      {isSignup ? (
        <label className="flex items-start gap-2 text-sm text-[var(--color-text)]">
          <input
            type="checkbox"
            checked={Boolean(fields.termsAccepted)}
            onChange={(e) => updateField("termsAccepted", e.target.checked)}
            className="mt-0.5"
          />
          {t("termsLabel")}
        </label>
      ) : (
        <div className="flex items-center justify-between text-sm">
          <label className="flex items-center gap-2 text-[var(--color-text)]">
            <input
              type="checkbox"
              checked={Boolean(fields.rememberMe)}
              onChange={(e) => updateField("rememberMe", e.target.checked)}
            />
            {t("rememberMeLabel")}
          </label>
          <button type="button" onClick={() => setMode("forgot_email")} className="text-[var(--color-primary)] hover:underline">
            {t("forgotPasswordLink")}
          </button>
        </div>
      )}
      {isSignup && <InlineFieldError message={fieldErrorMessage("termsAccepted")} />}

      <Button type="submit" disabled={isSubmitting}>
        {isSignup ? t("signupSubmit") : t("loginSubmit")}
      </Button>

      {showOAuth && <OAuthButtonGroup onSelect={(provider) => void submitOAuth(provider)} disabled={isSubmitting} />}
    </form>
  );
}
