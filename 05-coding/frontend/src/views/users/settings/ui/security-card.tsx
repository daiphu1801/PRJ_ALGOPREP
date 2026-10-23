// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Entry to the change-password dialog from Cài đặt, next to the other account groups. The dialog is
// the same feature the profile screen opens; this card only decides when to show it.
"use client";

import { useState } from "react";
import { KeyRound } from "lucide-react";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { Button, Card, SettingRow } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";
import { ChangePasswordDialog } from "@/features/change-my-password";

export function SecurityCard({ profile }: { profile: UserProfile }) {
  const t = useT("settings");
  const tp = useT("profile");
  const [open, setOpen] = useState(false);
  const { hasPassword } = profile;

  // Q3 closed 2026-10-18: `users.password_changed_at`, NULL = never changed.
  const description = hasPassword
    ? profile.passwordChangedAt
      ? tp("security.passwordChangedAt", {
          date: formatDateVN(profile.passwordChangedAt),
        })
      : tp("security.passwordNoDate")
    : tp("security.noPassword", {
        provider: profile.linkedProviders[0] ?? "OAuth",
      });

  return (
    <Card title={t("security.title")}>
      <SettingRow label={t("security.passwordLabel")} description={description}>
        <Button
          size="sm"
          variant="ghost"
          className="border border-[var(--color-border)]"
          onClick={() => setOpen(true)}
        >
          <KeyRound className="mr-1.5 h-4 w-4" aria-hidden="true" />
          {hasPassword ? t("security.changeBtn") : t("security.setBtn")}
        </Button>
      </SettingRow>
      {open ? (
        <ChangePasswordDialog
          hasPassword={hasPassword}
          onClose={() => setOpen(false)}
        />
      ) : null}
    </Card>
  );
}
