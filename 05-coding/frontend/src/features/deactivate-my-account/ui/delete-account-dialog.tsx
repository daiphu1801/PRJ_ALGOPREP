// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { ConfirmDialog } from "@/shared/ui";
import { useDeactivateAccount } from "../model/use-deactivate-account";

export function DeleteAccountDialog({
  email,
  onClose,
}: {
  email: string;
  onClose: () => void;
}) {
  const t = useT("settings");
  const { confirmPhrase, setConfirmPhrase, canConfirm, isSubmitting, confirm } =
    useDeactivateAccount(email);

  async function handleConfirm() {
    const result = await confirm();
    if (result.status === "done") toast.success(t("deleteConfirm.done"));
    else if (result.status === "error") toast.error(t(result.errorKey));
  }

  return (
    <ConfirmDialog
      open
      onClose={onClose}
      onConfirm={() => void handleConfirm()}
      title={t("deleteConfirm.title")}
      confirmLabel={t("deleteConfirm.confirm")}
      cancelLabel={t("deleteConfirm.cancel")}
      destructive
      // Sheet 6 Khu vực I NO 5: only enabled once the typed phrase matches the account email —
      // `ConfirmDialog` has no separate "disabled" prop, so `pending` also carries that gate here.
      pending={isSubmitting || !canConfirm}
    >
      <p className="mb-3">{t("dangerZone.consequenceText")}</p>
      <label
        htmlFor="confirm-phrase"
        className="mb-1 block text-xs font-medium text-[var(--color-text-muted)]"
      >
        {t("deleteConfirm.confirmPhraseLabel", { email })}
      </label>
      <input
        id="confirm-phrase"
        value={confirmPhrase}
        onChange={(e) => setConfirmPhrase(e.target.value)}
        className="h-9 w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text)]"
      />
    </ConfirmDialog>
  );
}
