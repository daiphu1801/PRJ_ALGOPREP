// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Read-only sign-in methods. Changing the password lives in Cài đặt (owner decision 2026-10-20).
"use client";

import { useT } from "@/shared/i18n";
import { Badge, Card, SettingRow } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

export function SecurityCard({ profile }: { profile: UserProfile }) {
  const t = useT("profile");

  return (
    <Card title={t("security.title")}>
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
    </Card>
  );
}
