// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { ADMIN_NAV_MISC } from "../model/admin-nav";
import { AreaToolbar } from "./area-toolbar";

/**
 * Real identity waits on app/providers/auth-provider.tsx session bootstrap (still TODO) — shown as
 * a static two-line placeholder (name + role, dc.html:116-119) so the row's shape matches the
 * prototype without inventing a specific fake person's name.
 */
export function AdminToolbar() {
  const t = useT("adminNav");
  return (
    <AreaToolbar
      name={t("identityNamePlaceholder")}
      roleLabel={t("adminIdentityPlaceholder")}
      items={ADMIN_NAV_MISC.map(({ key, href, labelKey, icon }) => ({
        key,
        href,
        label: t(labelKey),
        icon,
      }))}
    />
  );
}
