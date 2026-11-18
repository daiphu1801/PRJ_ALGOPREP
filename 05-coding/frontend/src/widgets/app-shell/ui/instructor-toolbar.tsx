// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useMyProfile } from "@/entities/user";
import { useT } from "@/shared/i18n";
import { INSTRUCTOR_NAV_MISC } from "../model/instructor-nav";
import { AreaToolbar } from "./area-toolbar";

/** Same toolbar row as the Admin area; the name comes from the mock profile of the instructor. */
export function InstructorToolbar() {
  const t = useT("instructorNav");
  const profile = useMyProfile("instructor").data;
  const name = profile?.displayName ?? "";
  return (
    <AreaToolbar
      name={name}
      userId={profile?.userId}
      avatarVersion={profile?.avatarVersion}
      roleLabel={t("roleLabel")}
      items={INSTRUCTOR_NAV_MISC.map(({ key, href, labelKey, icon }) => ({
        key,
        href,
        label: t(labelKey),
        icon,
      }))}
    />
  );
}
