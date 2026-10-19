// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { ADMIN_NAV_GROUPS, ADMIN_NAV_OVERVIEW } from "../model/admin-nav";
import { GroupedSidebar } from "./grouped-sidebar";

/** Admin area sidebar: the shared grouped layout with the 11 Admin destinations (BD ADM0101 section 6). */
export function AdminSidebar() {
  return (
    <GroupedSidebar
      namespace="adminNav"
      overview={ADMIN_NAV_OVERVIEW}
      groups={ADMIN_NAV_GROUPS}
      collapsedStorageKey="algoprep-admin-collapsed"
    />
  );
}
