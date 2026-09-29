// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The student top-nav, shared by every Student screen — each prototype carries the identical
// header block (09-layoutBase/Dashboard AlgoPrep.dc.html:50-92, repeated across all of them).
//
// SOURCE OF TRUTH IS 02-bd/screens/users/_shell.md mục 2.2-2.3, NOT the prototype. An earlier pass
// built this list from the `.dc.html` files alone and got four things wrong (dropped Workspace and
// Phỏng vấn giả lập as "they need an id", moved Tiến độ onto the main nav, shrank the user menu to
// four items). `_shell.md` mục 6 Q2 had already answered the id question — see the Workspace entry
// below — and the prototype is layout evidence, not a specification (SoT ladder: P3 `02-bd`
// outranks P4).
//
// Two amendments on top of that BD, both owner instructions recorded as decisions:
//  - "Tổng quan" is BACK as the first item, pointing at the real `/dashboard`. `_shell.md` mục 2.2
//    and mục 6 Q1 had removed it (`DEC-2026-0922-users-and-admin-conflict-resolutions`) because it
//    only pointed at a static mock file with no slug; `DEC-2026-0927-student-dashboard-home` then
//    created that slug, superseding that half of the 2026-09-22 decision.
//  - "Tiến độ của tôi" is NOT in the user menu, because the screen no longer exists on its own:
//    `my_progress` was merged into `dashboard` by
//    `DEC-2026-0927-student-area-merge-and-shared-shell`.
import type { LucideIcon } from "lucide-react";
import { Bookmark, LogOut, Settings, UserCircle } from "lucide-react";

export type StudentNavItem = {
  key: string;
  href: string;
  /** Key inside the `nav` message namespace. */
  labelKey: string;
};

export const STUDENT_NAV_ITEMS: StudentNavItem[] = [
  { key: "dashboard", href: "/dashboard", labelKey: "dashboard" },
  { key: "problems", href: "/problems", labelKey: "problems" },
  // `/workspace` carries no id on purpose: _shell.md mục 6 Q2 (closed 2026-09-22) says this entry
  // opens the most recent unfinished draft and falls back to the problem list when there is none.
  // The route itself does that redirect — app/(student)/workspace/page.tsx.
  { key: "workspace", href: "/workspace", labelKey: "workspace" },
  { key: "submissions", href: "/submissions", labelKey: "submissions" },
  // Likewise id-less: MockInterviewView opens on its own entry screen (pick a source, configure the
  // session) and never read the submissionId the old route passed it.
  { key: "mockInterview", href: "/mock-interview", labelKey: "mockInterview" },
  { key: "interviewBank", href: "/interview-bank", labelKey: "interviewBank" },
];

export type StudentMenuItem = StudentNavItem & {
  icon: LucideIcon;
  /** Rendered in the danger color and separated by a rule, like dc.html:89-91. */
  danger?: boolean;
};

/** The avatar dropdown, _shell.md mục 2.3 minus the merged-away `my_progress` entry. */
export const STUDENT_MENU_ITEMS: StudentMenuItem[] = [
  { key: "profile", href: "/profile", labelKey: "profile", icon: UserCircle },
  { key: "saved", href: "/saved", labelKey: "saved", icon: Bookmark },
  { key: "settings", href: "/settings", labelKey: "settings", icon: Settings },
  { key: "logout", href: "/login", labelKey: "logout", icon: LogOut, danger: true },
];
