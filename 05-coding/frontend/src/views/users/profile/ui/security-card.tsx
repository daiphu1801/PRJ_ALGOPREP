// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Read-only sign-in methods. Changing the password lives in Cài đặt (owner decision 2026-10-20).
"use client";

import { KeyRound, ShieldCheck } from "lucide-react";
import { useT } from "@/shared/i18n";
import { Badge, Card } from "@/shared/ui";
import type { UserProfile } from "@/entities/user";

export function SecurityCard({ profile }: { profile: UserProfile }) {
  const t = useT("profile");
  const providers = profile.linkedProviders;

  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <ShieldCheck
            size={18}
            aria-hidden="true"
            className="text-[var(--color-primary)]"
          />
          {t("security.title")}
        </span>
      }
    >
      <p className="text-sm text-[var(--color-text-muted)]">
        {t("security.providerLabel")}
      </p>
      {profile.hasPassword || providers.length > 0 ? (
        <ul className="mt-2 flex flex-wrap gap-2">
          {profile.hasPassword ? (
            <li>
              <Badge className="gap-1.5 px-2.5 py-1.5 text-xs">
                <KeyRound size={14} aria-hidden="true" />
                {t("security.providerCredentials")}
              </Badge>
            </li>
          ) : null}
          {providers.map((provider) => (
            <li key={provider}>
              <Badge className="px-2.5 py-1.5 text-xs">
                {provider === "GOOGLE" ? "Google" : "GitHub"}
              </Badge>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
