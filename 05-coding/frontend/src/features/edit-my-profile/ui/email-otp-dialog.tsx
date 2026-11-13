// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// BD-added popup (prototype has none, 02-bd/screens/users/USR0502_profile.md Khu vực G). Shows the
// 10-minute code validity countdown and a resend button with a 60-second cooldown; resend calls the
// same request endpoint again (DD F1 #11), which enforces the cooldown (IDT-V12) and the quota of 3
// per 24 hours (IDT-V05).
"use client";

import { useEffect, useState } from "react";
import { useT } from "@/shared/i18n";
import { useApiErrorMessage } from "@/shared/i18n/use-api-error-message";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal } from "@/shared/ui";
import {
  confirmEmailChange,
  emailOtpSchema,
  requestEmailChange,
} from "@/entities/user";

/** Server-side values of DD F1 #11: code lifetime 600 s, resend cooldown 60 s. */
const CODE_TTL_MS = 600_000;
const RESEND_COOLDOWN_MS = 60_000;

function formatClock(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000));
  const mm = String(Math.floor(total / 60)).padStart(2, "0");
  return `${mm}:${String(total % 60).padStart(2, "0")}`;
}

export function EmailOtpDialog({
  newEmail,
  onCancel,
  onConfirmed,
}: {
  newEmail: string;
  onCancel: () => void;
  onConfirmed: () => void;
}) {
  const t = useT("profile");
  const apiMessage = useApiErrorMessage();
  const [code, setCode] = useState("");
  // Red border only; the message is a toast.
  const [invalid, setInvalid] = useState(false);
  const [pending, setPending] = useState(false);
  // The code was sent just before this dialog opened.
  const [expiresAt, setExpiresAt] = useState(() => Date.now() + CODE_TTL_MS);
  const [cooldownUntil, setCooldownUntil] = useState(
    () => Date.now() + RESEND_COOLDOWN_MS,
  );
  const [now, setNow] = useState(() => Date.now());
  const [resending, setResending] = useState(false);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const expired = now >= expiresAt;
  const cooldownLeft = Math.max(0, cooldownUntil - now);

  const resend = async () => {
    setResending(true);
    try {
      await requestEmailChange(newEmail);
      const sentAt = Date.now();
      setNow(sentAt);
      setExpiresAt(sentAt + CODE_TTL_MS);
      setCooldownUntil(sentAt + RESEND_COOLDOWN_MS);
      setCode("");
      setInvalid(false);
      toast.success(t("emailOtp.resent"));
    } catch (error) {
      // Cooldown (IDT-V12), quota (IDT-V05), taken address (IDT-415) or outage.
      toast.error(apiMessage(error));
    } finally {
      setResending(false);
    }
  };

  const submit = async () => {
    const parsed = emailOtpSchema.safeParse({ code });
    if (!parsed.success) {
      setInvalid(true);
      toast.error(t(parsed.error.issues[0]!.message));
      return;
    }
    setPending(true);
    try {
      const outcome = await confirmEmailChange(parsed.data.code, newEmail);
      if (!outcome.ok) {
        setInvalid(true);
        toast.error(t(outcome.message));
        return;
      }
      toast.success(t("emailOtp.done"));
      onConfirmed();
    } catch (error) {
      // Taken address, rate limit or outage: not a wrong code, so no red border.
      toast.error(apiMessage(error));
    } finally {
      setPending(false);
    }
  };

  return (
    <Modal
      open
      onClose={onCancel}
      title={t("emailOtp.title")}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={onCancel}
            disabled={pending}
          >
            {t("emailOtp.cancel")}
          </Button>
          <Button
            size="sm"
            onClick={submit}
            disabled={pending || expired || code.length !== 6}
            aria-busy={pending || undefined}
          >
            {t("emailOtp.confirm")}
          </Button>
        </>
      }
    >
      <p className="mb-3">{t("emailOtp.hint", { email: newEmail })}</p>
      <input
        aria-label={t("emailOtp.codeLabel")}
        inputMode="numeric"
        maxLength={6}
        value={code}
        onChange={(e) => {
          setCode(e.target.value.replace(/\D/g, ""));
          setInvalid(false);
        }}
        aria-invalid={invalid || undefined}
        className={`h-10 w-full rounded-lg border bg-[var(--color-surface)] px-3 text-center text-lg tracking-[0.4em] ${invalid ? "border-[var(--color-danger)]" : "border-[var(--color-border)]"}`}
      />
      <div className="mt-2 flex items-center justify-between gap-3 text-sm">
        <span
          role="timer"
          className={
            expired
              ? "text-[var(--color-danger)]"
              : "text-[var(--color-text-muted)]"
          }
        >
          {expired
            ? t("emailOtp.expired")
            : t("emailOtp.expiresIn", { time: formatClock(expiresAt - now) })}
        </span>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => void resend()}
          disabled={pending || resending || cooldownLeft > 0}
        >
          {cooldownLeft > 0
            ? t("emailOtp.resendCooldown", {
                seconds: Math.ceil(cooldownLeft / 1000),
              })
            : t("emailOtp.resend")}
        </Button>
      </div>
    </Modal>
  );
}
