/**
 * User account as the admin management screen sees it (F1-13: change role, lock/unlock, reset
 * password). A3 edits accounts here; creating one is a separate open question — see the debt note
 * in the view.
 */
export type AdminUserRole = "student" | "instructor" | "admin";

export type AdminUserStatus = "active" | "pending" | "locked";

export type AdminUser = {
  /** Server id, used by every action call; the mock uses the e-mail. */
  id: string;
  /** Email doubles as the row key — the mockup keys its selection map the same way. */
  email: string;
  name: string;
  role: AdminUserRole;
  /** Absent with the real API: no read model yet (F4), so nothing is shown instead of a made-up zero. */
  solvedCount?: number;
  submissionCount?: number;
  /**
   * Human-readable "last seen". NO DATA SOURCE EXISTS for this yet: `database/F1_identity.md` has no
   * "last active" column, only `refresh_tokens.issued_at` as an indirect proxy. Flagged as level B1
   * in 07-review/bd_screens_admin_open_questions_260913.md, which proposes adding
   * `users.last_active_at`. Kept as a plain string here so the mock cannot be mistaken for a
   * resolved contract.
   */
  lastActiveLabel?: string;
  /** Real API: ISO time of the latest activity, `null` when the account never logged in. */
  lastActiveAt?: string | null;
  status: AdminUserStatus;
  /**
   * `password_hash IS NOT NULL` on the server (DD `ListUsers`, DEC-2026-1011). Absent means true;
   * false marks an OAuth-only account, whose reset sends provider guidance instead of a temporary password.
   */
  hasPassword?: boolean;
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
  /** Email of the acting admin; the lock rules compare against it. */
  currentUserEmail: string;
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
