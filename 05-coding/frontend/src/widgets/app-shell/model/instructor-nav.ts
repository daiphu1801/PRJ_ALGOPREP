// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The 5 Instructor nav destinations, in the order and with the labels the prototype's own sidebar
// uses (09-layoutBase/Giáo viên - Tổng quan.dc.html:245-251 `navDefs`), plus the "KHÁC" group
// below it (:262-265). Every href points at a route that already exists under
// app/(instructor)/** or app/(student)/**, never at a 404.
//
// Two deliberate divergences from the mockup, both already precedented on the Admin side:
//   - Icons are lucide glyphs, not the mockup's 2-letter mono chips (TQ/LH/BT/CB/TĐ). Reuses the
//     shared NavLink primitive instead of forking it for one area, and keeps both authenticated
//     areas visually consistent.
//   - The unread badges (3 / 18 / 9 at dc.html:246-249) are hardcoded constants in the mockup.
//     Rendering them would need counts no entity exposes for the sidebar yet, so they are left out
//     rather than invented.
import type { LucideIcon } from "lucide-react";
import {
  BookOpen,
  ClipboardCheck,
  GraduationCap,
  LayoutDashboard,
  Settings,
  UserCircle,
  Users,
} from "lucide-react";

export type InstructorNavItem = {
  key: string;
  href: string;
  /** Resolves under the `instructorNav` message namespace. */
  labelKey: string;
  icon: LucideIcon;
};

export const INSTRUCTOR_NAV_MAIN: InstructorNavItem[] = [
  {
    key: "overview",
    href: "/instructor/overview",
    labelKey: "overview",
    icon: LayoutDashboard,
  },
  {
    key: "classes",
    href: "/instructor/classes",
    labelKey: "classes",
    icon: Users,
  },
  {
    key: "assignments",
    href: "/instructor/assignments",
    labelKey: "assignments",
    icon: BookOpen,
  },
  {
    key: "grading",
    href: "/instructor/grading",
    labelKey: "grading",
    icon: ClipboardCheck,
  },
  // Top-level route, not a child of /instructor/classes: since 2026-09-27 this is the student
  // list for ALL classes, with ?classId= only as a preselected filter.
  {
    key: "students",
    href: "/instructor/students",
    labelKey: "students",
    icon: GraduationCap,
  },
];

export const INSTRUCTOR_NAV_MISC: InstructorNavItem[] = [
  { key: "settings", href: "/settings", labelKey: "settings", icon: Settings },
  { key: "profile", href: "/profile", labelKey: "profile", icon: UserCircle },
];
