// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Wording matches the RD-corrected consequence text (soft-lock → anonymize after grace period),
// NOT the prototype's original "xoá vĩnh viễn..." line — 01-rd/screens/users/USR0503_settings.md
// mục 4 Q2, F1-16.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { Button, Card } from "@/shared/ui";
import { DeleteAccountDialog } from "@/features/deactivate-my-account";

export function DangerZoneCard({ email }: { email: string }) {
  const t = useT("settings");
  const [open, setOpen] = useState(false);

  return (
    <Card
      title={t("dangerZone.title")}
      className="border-[var(--color-danger)]"
    >
      <div className="flex flex-wrap items-center gap-4">
        <p className="min-w-0 flex-1 text-sm text-[var(--color-text-muted)]">{t("dangerZone.consequenceText")}</p>
        <Button
          variant="ghost"
          size="sm"
          className="border border-[var(--color-danger)] text-[var(--color-danger)] hover:bg-[var(--color-danger)] hover:text-[var(--color-background)]"
          onClick={() => setOpen(true)}
        >
          {t("dangerZone.delete")}
        </Button>
      </div>
      {open ? <DeleteAccountDialog email={email} onClose={() => setOpen(false)} /> : null}
    </Card>
  );
}
