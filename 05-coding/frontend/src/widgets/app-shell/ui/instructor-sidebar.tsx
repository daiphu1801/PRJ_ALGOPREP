// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Suspense } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import {
  parseSubmissionOrigin,
  SUBMISSION_ORIGIN_NAV_HREF,
} from "@/entities/submission";
import {
  INSTRUCTOR_NAV_GROUPS,
  INSTRUCTOR_NAV_OVERVIEW,
} from "../model/instructor-nav";
import { GroupedSidebar } from "./grouped-sidebar";

/**
 * Instructor sidebar. Since 2026-10-11 (owner instruction: the instructor UI follows the admin
 * layout) it is the same grouped sidebar as the Admin area: logo + collapse toggle, an overview
 * link, collapsible groups, the theme switcher at the foot. Settings, profile and sign-out moved
 * into the toolbar user menu, like Admin. The "GIÁO VIÊN" eyebrow stays, it is what tells the two
 * areas apart (09-layoutBase/Giáo viên - Tổng quan.dc.html:69). Collapse state persists under its
 * own `algoprep-teacher-collapsed` key.
 *
 * The read-only submission result (/instructor/submissions/<id>) keeps lit the item of the screen that
 * opened it, named by `?from=` (DEC-2026-1014-instructor-submissions-list-details).
 */
function SidebarBody({ activeHrefOverride }: { activeHrefOverride?: string }) {
  return (
    <GroupedSidebar
      namespace="instructorNav"
      overview={INSTRUCTOR_NAV_OVERVIEW}
      groups={INSTRUCTOR_NAV_GROUPS}
      collapsedStorageKey="algoprep-teacher-collapsed"
      eyebrowKey="areaEyebrow"
      matchPrefix
      defaultOpenAll
      activeHrefOverride={activeHrefOverride}
    />
  );
}

/**
 * Pages reached from more than one screen keep the item of the calling screen lit, named by `?from=`:
 * - the result page of one submission (/instructor/submissions/<id>, not the list itself);
 * - the student detail (/instructor/classes/<id>/students/<id>): from=students (the student list) or
 *   from=submissions; any other value, or none, keeps "Lớp của tôi" through the pathname match.
 */
export function instructorActiveHrefOverride(
  pathname: string,
  from: string | null,
): string | undefined {
  if (/^\/instructor\/submissions\/[^/]+$/.test(pathname)) {
    return SUBMISSION_ORIGIN_NAV_HREF[parseSubmissionOrigin(from)];
  }
  if (/^\/instructor\/classes\/[^/]+\/students\/[^/]+$/.test(pathname)) {
    if (from === "students") return "/instructor/students";
    if (from === "submissions") return "/instructor/submissions";
  }
  return undefined;
}

function SidebarWithOrigin() {
  const pathname = usePathname();
  const from = useSearchParams().get("from");
  return (
    <SidebarBody
      activeHrefOverride={instructorActiveHrefOverride(pathname, from)}
    />
  );
}

export function InstructorSidebar() {
  // useSearchParams needs a Suspense boundary; until it resolves the plain pathname match is shown.
  return (
    <Suspense fallback={<SidebarBody />}>
      <SidebarWithOrigin />
    </Suspense>
  );
}
