// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The 11 Admin nav destinations from 02-bd/screens/admin/admin_overview.md section 6, grouped as
// the prototype groups them (Nội dung/Vận hành/AI/Hệ thống — dòng 386-389 of
// 09-layoutBase/Admin - Tổng quan.dc.html). Links point at routes that already exist as stub
// pages (app/(admin)/admin/**), never at a 404 — per this task's instruction. "Chấm lại"
// (admin_rejudge) is intentionally absent: removed from scope by
// DEC-2026-0828-remove-rejudge-scope.
//
// i18n keys resolve under the `adminNav` message namespace. Icons are 1:1 with the prototype's
// `data-lucide` attributes (dc.html:359-406) — the overview item and each GROUP header carry one;
// individual items inside a group and none of `ADMIN_NAV_MISC` do NOT (dc.html only ever puts an
// `<i data-lucide>` on `grp.icon`/`ol.icon`, never on a plain `itm` row) — flagged by the owner
// 2026-09-15 asking for a 1:1 icon pass, not "add icons everywhere".
import type { LucideIcon } from "lucide-react";
import { BookOpen, LayoutDashboard, ListChecks, LogIn, Settings, Shield, Sparkles, UserCircle } from "lucide-react";

export type AdminNavItem = {
  key: string;
  href: string;
  labelKey: string;
};

export type AdminNavMiscItem = AdminNavItem & {
  icon: LucideIcon;
};

export type AdminNavGroup = {
  key: string;
  labelKey: string;
  icon: LucideIcon;
  items: AdminNavItem[];
};

/** Ungrouped, always-visible first item — the entry point after ADMIN login (BD section 6). */
export const ADMIN_NAV_OVERVIEW: AdminNavItem & { icon: LucideIcon } = {
  key: "overview",
  href: "/admin/overview",
  labelKey: "overview",
  icon: LayoutDashboard,
};

export const ADMIN_NAV_GROUPS: AdminNavGroup[] = [
  {
    key: "content",
    labelKey: "groupContent",
    icon: BookOpen,
    items: [
      { key: "problems", href: "/admin/problems", labelKey: "problems" },
      { key: "interviewQuestions", href: "/admin/interview-questions", labelKey: "interviewQuestions" },
    ],
  },
  {
    key: "operations",
    labelKey: "groupOperations",
    icon: ListChecks,
    items: [
      { key: "queue", href: "/admin/queue", labelKey: "queue" },
      { key: "languageConfig", href: "/admin/language-config", labelKey: "languageConfig" },
    ],
  },
  {
    key: "ai",
    labelKey: "groupAi",
    icon: Sparkles,
    items: [
      { key: "aiConfig", href: "/admin/ai-config", labelKey: "aiConfig" },
      { key: "aiUsage", href: "/admin/ai-usage", labelKey: "aiUsage" },
    ],
  },
  {
    key: "system",
    labelKey: "groupSystem",
    icon: Shield,
    items: [
      { key: "users", href: "/admin/users", labelKey: "users" },
      { key: "systemLog", href: "/admin/system-log", labelKey: "systemLog" },
      { key: "permissions", href: "/admin/permissions", labelKey: "permissions" },
    ],
  },
];

/**
 * The "KHÁC" footer group (BD section 2 point 6, section 6 — dòng 403-407). `settings` here
 * intentionally reuses the `languageConfig` route, matching a known quirk BD flags for DD to fix
 * (the prototype's "Cài đặt" link points at the wrong destination) — kept as-is rather than
 * silently invented, [SoT: 02-bd/screens/admin/admin_overview.md section 2 point 6].
 */
export const ADMIN_NAV_MISC: AdminNavMiscItem[] = [
  { key: "settings", href: "/admin/language-config", labelKey: "settings", icon: Settings },
  { key: "auth", href: "/login", labelKey: "auth", icon: LogIn },
  { key: "profile", href: "/profile", labelKey: "profile", icon: UserCircle },
];
