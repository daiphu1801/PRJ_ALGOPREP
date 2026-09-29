/**
 * User account as the admin management screen sees it (F1-13: change role, lock/unlock, reset
 * password). A3 edits accounts here; creating one is a separate open question — see the debt note
 * in the view.
 */
export type AdminUserRole = "student" | "instructor" | "admin";

export type AdminUserStatus = "active" | "pending" | "locked";

export type AdminUser = {
  /** Email doubles as the row key — the mockup keys its selection map the same way. */
  email: string;
  name: string;
  role: AdminUserRole;
  solvedCount: number;
  submissionCount: number;
  /**
   * Human-readable "last seen". NO DATA SOURCE EXISTS for this yet: `database/identity.md` has no
   * "last active" column, only `refresh_tokens.issued_at` as an indirect proxy. Flagged as level B1
   * in 07-review/bd_screens_admin_open_questions_260913.md, which proposes adding
   * `users.last_active_at`. Kept as a plain string here so the mock cannot be mistaken for a
   * resolved contract.
   */
  lastActiveLabel: string;
  status: AdminUserStatus;
};

export type AdminUserStat = {
  key: string;
  value: string;
  delta: string;
  deltaColorVar: string;
  meta: string;
};

export type RoleDistributionItem = {
  role: AdminUserRole | "deactivated";
  /** Pre-formatted "1.196 · 93%" — share and count together, as the mockup shows them. */
  label: string;
  /** Bar fill percentage, 0-100. */
  percent: number;
};

export type PendingTask = {
  key: string;
  count: number;
  colorVar: string;
};

export type AdminUserPage = {
  stats: AdminUserStat[];
  users: AdminUser[];
  roleDistribution: RoleDistributionItem[];
  pendingTasks: PendingTask[];
  /** Total accounts behind the paged list. */
  totalUsers: number;
  totalPages: number;
};

export function initialsOf(name: string): string {
  return name
    .split(" ")
    .slice(-2)
    .map((word) => word[0] ?? "")
    .join("");
}
