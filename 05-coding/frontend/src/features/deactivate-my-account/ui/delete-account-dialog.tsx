// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { ConfirmDialog, TextField } from "@/shared/ui";
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
      // Sheet 6 Khu vực I NO 5: confirm enabled only once the typed phrase matches the account
      // email; Cancel stays enabled. `pending` is reserved for the in-flight request.
      confirmDisabled={!canConfirm}
      pending={isSubmitting}
    >
      <p className="mb-3">{t("dangerZone.consequenceText")}</p>
      <TextField
        label={t("deleteConfirm.confirmPhraseLabel", { email })}
        value={confirmPhrase}
        onChange={(e) => setConfirmPhrase(e.target.value)}
        invalid={confirmPhrase.trim() !== "" && !canConfirm}
        autoComplete="off"
      />
    </ConfirmDialog>
  );
}
