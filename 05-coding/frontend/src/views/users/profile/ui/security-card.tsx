// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { Badge, Button, Card, SettingRow } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";
import { ChangePasswordDialog } from "@/features/change-my-password";

export function SecurityCard({ profile }: { profile: UserProfile }) {
  const t = useT("profile");
  const [dialogOpen, setDialogOpen] = useState(false);

  // Sheet 5 Khu vực C NO 3 — Q3 (last-changed date column) is unresolved, so `passwordChangedAt`
  // being null just means "don't show a date", never a fabricated one.
  const passwordMeta = profile.hasPassword
    ? profile.passwordChangedAt
      ? t("security.passwordChangedAt", { date: profile.passwordChangedAt })
      : t("security.passwordNoDate")
    : t("security.noPassword", {
        provider: profile.linkedProviders[0] ?? "OAuth",
      });

  return (
    <Card title={t("security.title")}>
      <div className="flex flex-col gap-2">
        <SettingRow
          label={t("security.passwordLabel")}
          description={passwordMeta}
        >
          <Button size="sm" variant="ghost" onClick={() => setDialogOpen(true)}>
            {profile.hasPassword
              ? t("security.changePassword")
              : t("security.setPassword")}
          </Button>
        </SettingRow>
        <SettingRow
          label={t("security.providerLabel")}
          description={t("security.providerDescription")}
        >
          <div className="flex gap-1.5">
            {profile.hasPassword ? (
              <Badge>{t("security.providerCredentials")}</Badge>
            ) : null}
            {profile.linkedProviders.map((provider) => (
              <Badge key={provider}>{provider}</Badge>
            ))}
          </div>
        </SettingRow>
      </div>
      {dialogOpen ? (
        <ChangePasswordDialog
          hasPassword={profile.hasPassword}
          onClose={() => setDialogOpen(false)}
        />
      ) : null}
    </Card>
  );
}
