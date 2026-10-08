import type { AdminUser, AdminUserRole } from "./types";

export type LockCheck = {
  /** Locking would leave no active ADMIN: hard block (ADM0201 Q6, DEC-2026-1001-admin-configurable-settings). */
  blockedLastAdmin: boolean;
  /** The acting admin is among the targets: allowed, but needs an explicit warning. */
  includesSelf: boolean;
};

/**
 * Splits a selection into the rows each status button would actually change.
 *
 * A single selection can hold both active and locked rows, so one button rarely covers it. Returning
 * the two sets lets the screen disable the button with nothing to do (BD section 6, Khu vực C NO 8:
 * the lock button needs at least one `ACTIVE` row) instead of firing a call that changes nothing.
 */
export function partitionByStatus(
  users: readonly AdminUser[],
  targetEmails: ReadonlySet<string>,
): { lockable: AdminUser[]; unlockable: AdminUser[] } {
  const lockable: AdminUser[] = [];
  const unlockable: AdminUser[] = [];
  for (const user of users) {
    if (!targetEmails.has(user.email)) continue;
    if (user.status === "locked") unlockable.push(user);
    else lockable.push(user);
  }
  return { lockable, unlockable };
}

/**
 * Pure rule check for locking accounts. "At least one active ADMIN always remains" is kept as a hard
 * rule because it protects the ability to administer anything; locking yourself is only a warning.
 */
export function checkLock(
  users: readonly AdminUser[],
  targetEmails: ReadonlySet<string>,
  currentEmail: string,
): LockCheck {
  const activeAdmins = users.filter(
    (user) => user.role === "admin" && user.status === "active",
  );
  const remaining = activeAdmins.filter(
    (user) => !targetEmails.has(user.email),
  );
  return {
    blockedLastAdmin:
      activeAdmins.some((user) => targetEmails.has(user.email)) &&
      remaining.length === 0,
    includesSelf: targetEmails.has(currentEmail),
  };
}

/**
 * Rule check for changing ONE account's role. Same two rules as locking (ADM0201 Q6): demoting the
 * last active ADMIN is a hard block, demoting yourself is only a warning.
 */
export function checkRoleChange(
  users: readonly AdminUser[],
  email: string,
  nextRole: AdminUserRole,
  currentEmail: string,
): LockCheck {
  const target = users.find((user) => user.email === email);
  const losesAdmin = target?.role === "admin" && nextRole !== "admin";
  const remaining = users.filter(
    (user) =>
      user.role === "admin" && user.status === "active" && user.email !== email,
  );
  return {
    blockedLastAdmin:
      losesAdmin && target?.status === "active" && remaining.length === 0,
    includesSelf: losesAdmin && email === currentEmail,
  };
}
