// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Instructor navigation in the same shape as the Admin one (admin-nav.ts): an overview link, then
// collapsible groups, then the "misc" links that live in the toolbar user menu. Regrouped
// 2026-10-11 on the owner's instruction that the instructor layout follows the admin layout; the
// 5 destinations of 09-layoutBase/Giáo viên - Tổng quan.dc.html:245-251 `navDefs` and the "Nội dung"
// group of DEC-2026-0922-users-and-admin-conflict-resolutions are all still here, only the
// grouping is new. Every href points at a route that exists under app/(instructor)/**.
//
// Group icons are lucide glyphs on the group headers only (items carry none), like Admin. The
// unread badges (3 / 18 / 9) of the mockup are dropped for good (DEC-2026-1011-teacher-nav-badge-dropped).
import {
  BookOpen,
  ClipboardCheck,
  LayoutDashboard,
  LogOut,
  Settings,
  UserCircle,
  Users,
} from "lucide-react";
import type {
  AdminNavGroup,
  AdminNavItem,
  AdminNavMiscItem,
} from "./admin-nav";
import type { LucideIcon } from "lucide-react";

/** Resolves under the `instructorNav` message namespace. */
export const INSTRUCTOR_NAV_OVERVIEW: AdminNavItem & { icon: LucideIcon } = {
  key: "overview",
  href: "/instructor/overview",
  labelKey: "overview",
  icon: LayoutDashboard,
};

export const INSTRUCTOR_NAV_GROUPS: AdminNavGroup[] = [
  {
    key: "classes",
    labelKey: "groupClasses",
    icon: Users,
    items: [
      { key: "classes", href: "/instructor/classes", labelKey: "classes" },
      // Top-level route, not a child of /instructor/classes: since 2026-09-27 this is the student
      // list for ALL classes, with ?classId= only as a preselected filter.
      { key: "students", href: "/instructor/students", labelKey: "students" },
    ],
  },
  {
    key: "work",
    labelKey: "groupWork",
    icon: ClipboardCheck,
    items: [
      {
        key: "assignments",
        href: "/instructor/assignments",
        labelKey: "assignments",
      },
      {
        key: "submissions",
        href: "/instructor/submissions",
        labelKey: "submissions",
      },
    ],
  },
  {
    key: "content",
    labelKey: "groupContent",
    icon: BookOpen,
    items: [
      { key: "problems", href: "/instructor/problems", labelKey: "problems" },
      {
        key: "interviewQuestions",
        href: "/instructor/interview-questions",
        labelKey: "interviewQuestions",
      },
    ],
  },
];

/** Toolbar user menu: own settings and profile stay in the instructor area, "Thoát" signs out. */
export const INSTRUCTOR_NAV_MISC: AdminNavMiscItem[] = [
  {
    key: "settings",
    href: "/instructor/settings",
    labelKey: "settings",
    icon: Settings,
  },
  {
    key: "profile",
    href: "/instructor/profile",
    labelKey: "profile",
    icon: UserCircle,
  },
  { key: "logout", href: "/login", labelKey: "logout", icon: LogOut },
];
