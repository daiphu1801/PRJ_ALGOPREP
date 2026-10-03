// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// BD-added popup (prototype has none, 02-bd/screens/users/USR0502_profile.md Khu vực G) — a fresh
// 10-minute countdown/resend-cooldown timer is intentionally NOT reproduced here (that machinery
// already exists in features/auth-by-credentials/ui/otp-input-group.tsx for the forgot-password
// flow); this is the simplified prototype-scope version, tracked as debt in the ledger row.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal } from "@/shared/ui";
import { confirmEmailChange, emailOtpSchema } from "@/entities/user";

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
  const [code, setCode] = useState("");
  // Red border only; the message is a toast.
  const [invalid, setInvalid] = useState(false);
  const [pending, setPending] = useState(false);

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
          <Button variant="ghost" size="sm" onClick={onCancel} disabled={pending}>
            {t("emailOtp.cancel")}
          </Button>
          <Button size="sm" onClick={submit} disabled={pending || code.length !== 6} aria-busy={pending || undefined}>
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
    </Modal>
  );
}
