// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The 11 Admin nav destinations from 02-bd/screens/admin/admin_overview.md section 6, grouped as
// the prototype groups them (Nội dung/Vận hành/AI/Hệ thống — dòng 386-389 of
// 09-layoutBase/Admin - Tổng quan.dc.html). Links point at routes that already exist as stub
// pages (app/(admin)/admin/**), never at a 404 — per this task's instruction. "Chấm lại"
// (admin_rejudge) is intentionally absent: removed from scope by
// DEC-2026-0828-remove-rejudge-scope.
//
// i18n keys resolve under the `adminNav` message namespace.
export type AdminNavItem = {
  key: string;
  href: string;
  labelKey: string;
};

export type AdminNavGroup = {
  key: string;
  labelKey: string;
  items: AdminNavItem[];
};

/** Ungrouped, always-visible first item — the entry point after ADMIN login (BD section 6). */
export const ADMIN_NAV_OVERVIEW: AdminNavItem = {
  key: "overview",
  href: "/admin/overview",
  labelKey: "overview",
};

export const ADMIN_NAV_GROUPS: AdminNavGroup[] = [
  {
    key: "content",
    labelKey: "groupContent",
    items: [
      { key: "problems", href: "/admin/problems", labelKey: "problems" },
      { key: "interviewQuestions", href: "/admin/interview-questions", labelKey: "interviewQuestions" },
    ],
  },
  {
    key: "operations",
    labelKey: "groupOperations",
    items: [
      { key: "queue", href: "/admin/queue", labelKey: "queue" },
      { key: "languageConfig", href: "/admin/language-config", labelKey: "languageConfig" },
    ],
  },
  {
    key: "ai",
    labelKey: "groupAi",
    items: [
      { key: "aiConfig", href: "/admin/ai-config", labelKey: "aiConfig" },
      { key: "aiUsage", href: "/admin/ai-usage", labelKey: "aiUsage" },
    ],
  },
  {
    key: "system",
    labelKey: "groupSystem",
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
export const ADMIN_NAV_MISC: AdminNavItem[] = [
  { key: "settings", href: "/admin/language-config", labelKey: "settings" },
  { key: "auth", href: "/login", labelKey: "auth" },
  { key: "profile", href: "/profile", labelKey: "profile" },
];
