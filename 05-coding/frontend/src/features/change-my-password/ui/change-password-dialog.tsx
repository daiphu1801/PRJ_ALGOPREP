// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useState } from "react";
import { Check, Eye, EyeOff, Circle } from "lucide-react";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, TextField } from "@/shared/ui";
import { useChangePassword } from "../model/use-change-password";

const STRENGTH_COLOR = [
  "var(--color-admin-negative)",
  "var(--color-admin-negative)",
  "var(--color-admin-warn)",
  "var(--color-success)",
  "var(--color-success)",
];
const STRENGTH_KEY = ["weak", "weak", "fair", "good", "strong"] as const;

// Password field with a show/hide toggle inside the right edge.
function PasswordField({
  label,
  value,
  onChange,
  invalid,
  show,
  hide,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  invalid: boolean;
  show: string;
  hide: string;
}) {
  const [visible, setVisible] = useState(false);
  return (
    <TextField
      label={label}
      type={visible ? "text" : "password"}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      invalid={invalid}
      maxLength={128}
      trailing={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? hide : show}
          className="flex h-7 w-7 items-center justify-center rounded-md text-[var(--color-text-subtle)] hover:text-[var(--color-text)]"
        >
          {visible ? (
            <EyeOff className="h-4 w-4" aria-hidden="true" />
          ) : (
            <Eye className="h-4 w-4" aria-hidden="true" />
          )}
        </button>
      }
    />
  );
}

export function ChangePasswordDialog({
  hasPassword,
  onClose,
}: {
  hasPassword: boolean;
  onClose: () => void;
}) {
  const t = useT("profile");
  const {
    currentPassword,
    setCurrentPassword,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    fieldErrors,
    isSubmitting,
    submit,
  } = useChangePassword(hasPassword, onClose);

  const show = t("passwordDialog.show");
  const hide = t("passwordDialog.hide");
  const rules = [
    { key: "length", ok: newPassword.length >= 8 },
    { key: "letter", ok: /[A-Za-z]/.test(newPassword) },
    { key: "digit", ok: /\d/.test(newPassword) },
    {
      key: "match",
      ok: confirmPassword.length > 0 && confirmPassword === newPassword,
    },
  ];
  // Strength: one point per rule plus one for 12+ characters (shown only once something is typed).
  const score = Math.min(
    4,
    rules.slice(0, 3).filter((rule) => rule.ok).length +
      (newPassword.length >= 12 ? 1 : 0),
  );

  async function handleSubmit() {
    const result = await submit();
    if (result.ok)
      // First-time set on an OAuth account revokes no other session (owner 2026-10-21), so no sign-out hint.
      toast.success(
        t(hasPassword ? "passwordDialog.done" : "passwordDialog.doneSet"),
      );
    else toast.error(t(result.errorKey));
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={
        hasPassword
          ? t("passwordDialog.titleChange")
          : t("passwordDialog.titleSet")
      }
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            disabled={isSubmitting}
          >
            {t("passwordDialog.cancel")}
          </Button>
          <Button
            size="sm"
            onClick={() => void handleSubmit()}
            disabled={isSubmitting}
            aria-busy={isSubmitting || undefined}
          >
            {t("passwordDialog.confirm")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3.5">
        {hasPassword ? (
          <PasswordField
            label={t("passwordDialog.currentPassword")}
            value={currentPassword}
            onChange={setCurrentPassword}
            invalid={Boolean(fieldErrors.currentPassword)}
            show={show}
            hide={hide}
          />
        ) : null}
        <PasswordField
          label={t("passwordDialog.newPassword")}
          value={newPassword}
          onChange={setNewPassword}
          invalid={Boolean(fieldErrors.newPassword)}
          show={show}
          hide={hide}
        />
        {newPassword ? (
          <div>
            <div className="mb-1 flex items-center justify-between text-[12px]">
              <span className="text-[var(--color-text-muted)]">
                {t("passwordDialog.strengthLabel")}
              </span>
              <span
                className="font-semibold"
                style={{ color: STRENGTH_COLOR[score] }}
              >
                {t(`passwordDialog.strength.${STRENGTH_KEY[score]}`)}
              </span>
            </div>
            <div className="h-1.5 overflow-hidden rounded-full bg-[var(--color-track)]">
              <div
                className="h-full rounded-full transition-all"
                style={{
                  width: `${(score / 4) * 100}%`,
                  background: STRENGTH_COLOR[score],
                }}
              />
            </div>
          </div>
        ) : null}
        <PasswordField
          label={t("passwordDialog.confirmPassword")}
          value={confirmPassword}
          onChange={setConfirmPassword}
          invalid={Boolean(fieldErrors.confirmPassword)}
          show={show}
          hide={hide}
        />
        <ul className="flex flex-col gap-1.5 rounded-lg bg-[var(--color-surface-hover)] px-3 py-2.5">
          {rules.map((rule) => (
            <li
              key={rule.key}
              className={
                "flex items-center gap-2 text-[12.5px] " +
                (rule.ok
                  ? "text-[var(--color-success-text)]"
                  : "text-[var(--color-text-muted)]")
              }
            >
              {rule.ok ? (
                <Check className="h-3.5 w-3.5" aria-hidden="true" />
              ) : (
                <Circle className="h-3.5 w-3.5" aria-hidden="true" />
              )}
              {t(`passwordDialog.rule.${rule.key}`)}
            </li>
          ))}
        </ul>
      </div>
    </Modal>
  );
}
