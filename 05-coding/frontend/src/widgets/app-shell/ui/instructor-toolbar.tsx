// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useMyProfile } from "@/entities/user";
import { useT } from "@/shared/i18n";
import { INSTRUCTOR_NAV_MISC } from "../model/instructor-nav";
import { AreaToolbar } from "./area-toolbar";

/** Same toolbar row as the Admin area; the name comes from the mock profile of the instructor. */
export function InstructorToolbar() {
  const t = useT("instructorNav");
  const name = useMyProfile("instructor").data?.displayName ?? "";
  return (
    <AreaToolbar
      name={name}
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
