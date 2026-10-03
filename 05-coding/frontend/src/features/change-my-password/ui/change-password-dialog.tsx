// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, TextField } from "@/shared/ui";
import { useChangePassword } from "../model/use-change-password";

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

  async function handleSubmit() {
    const result = await submit();
    if (result.ok) toast.success(t("passwordDialog.done"));
    else toast.error(t(result.errorKey));
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={hasPassword ? t("passwordDialog.titleChange") : t("passwordDialog.titleSet")}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose} disabled={isSubmitting}>
            {t("passwordDialog.cancel")}
          </Button>
          <Button size="sm" onClick={() => void handleSubmit()} disabled={isSubmitting} aria-busy={isSubmitting || undefined}>
            {t("passwordDialog.confirm")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        {hasPassword ? (
          <TextField
            label={t("passwordDialog.currentPassword")}
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            invalid={Boolean(fieldErrors.currentPassword)}
            maxLength={128}
          />
        ) : null}
        <TextField
          label={t("passwordDialog.newPassword")}
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          invalid={Boolean(fieldErrors.newPassword)}
          maxLength={128}
        />
        <TextField
          label={t("passwordDialog.confirmPassword")}
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          invalid={Boolean(fieldErrors.confirmPassword)}
          maxLength={128}
        />
        <p className="text-xs text-[var(--color-text-subtle)]">{t("passwordDialog.ruleHint")}</p>
      </div>
    </Modal>
  );
}
