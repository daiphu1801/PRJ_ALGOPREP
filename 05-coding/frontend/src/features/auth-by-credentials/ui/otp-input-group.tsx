// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useEffect, useState } from "react";
import { useT } from "@/shared/i18n";
import { Button, InlineFieldError } from "@/shared/ui";

type OtpInputGroupProps = {
  value: string;
  onChange: (value: string) => void;
  error?: string;
  attemptsLeft: number;
  cooldownUntil: number;
  onResend: () => void;
  disabled?: boolean;
};

/**
 * 6-digit OTP entry for `forgot_otp` (02-bd/screens/shared/auth.md section 2, `OtpInputGroup`
 * — "chưa dựng trong prototype"). One text input rather than 6 separate boxes: same behavior,
 * far less state, acceptable simplification for the PROTOTYPE lane.
 */
export function OtpInputGroup({
  value,
  onChange,
  error,
  attemptsLeft,
  cooldownUntil,
  onResend,
  disabled,
}: OtpInputGroupProps) {
  const t = useT("auth");
  const [secondsLeft, setSecondsLeft] = useState(() => Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000)));

  useEffect(() => {
    const id = setInterval(() => {
      setSecondsLeft(Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000)));
    }, 1000);
    return () => clearInterval(id);
  }, [cooldownUntil]);

  return (
    <div>
      <label htmlFor="otp" className="mb-1 block text-sm font-medium text-[var(--color-text)]">
        {t("otpLabel")}
      </label>
      <input
        id="otp"
        inputMode="numeric"
        maxLength={6}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value.replace(/\D/g, ""))}
        className="w-full rounded-md border border-[var(--color-border)] bg-transparent px-3 py-2 text-center text-lg tracking-[0.5em] text-[var(--color-text)]"
        aria-describedby="otp-error"
      />
      <div id="otp-error">
        <InlineFieldError message={error} />
      </div>
      <p className="mt-1 text-xs text-[var(--color-text-muted)]">{t("otpAttemptsLeft", { count: attemptsLeft })}</p>
      <Button type="button" variant="ghost" size="sm" disabled={secondsLeft > 0 || disabled} onClick={onResend} className="mt-2">
        {secondsLeft > 0 ? t("otpResendCooldown", { seconds: secondsLeft }) : t("otpResend")}
      </Button>
    </div>
  );
}
