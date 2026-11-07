// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useMyProfile } from "@/entities/user";
import { useT } from "@/shared/i18n";
import { ADMIN_NAV_MISC } from "../model/admin-nav";
import { AreaToolbar } from "./area-toolbar";

/**
 * Name from the signed-in admin's profile (same read as the instructor toolbar); the static
 * placeholder (name + role, dc.html:116-119) shows while it loads, so the row never pretends to
 * know a person it has not read.
 */
export function AdminToolbar() {
  const t = useT("adminNav");
  const name = useMyProfile("admin").data?.displayName;
  return (
    <AreaToolbar
      name={name || t("identityNamePlaceholder")}
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
