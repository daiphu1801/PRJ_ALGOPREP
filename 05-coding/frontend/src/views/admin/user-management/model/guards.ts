import type { AdminUser } from "./types";

export type LockCheck = {
  /** Locking would leave no active ADMIN: hard block (ADM0201 Q6, DEC-2026-1001-admin-configurable-settings). */
  blockedLastAdmin: boolean;
  /** The acting admin is among the targets: allowed, but needs an explicit warning. */
  includesSelf: boolean;
};

/**
 * Pure rule check for locking accounts. "At least one active ADMIN always remains" is kept as a hard
 * rule because it protects the ability to administer anything; locking yourself is only a warning.
 */
export function checkLock(
  users: readonly AdminUser[],
  targetEmails: ReadonlySet<string>,
  currentEmail: string,
): LockCheck {
  const activeAdmins = users.filter((user) => user.role === "admin" && user.status === "active");
  const remaining = activeAdmins.filter((user) => !targetEmails.has(user.email));
  return {
    blockedLastAdmin:
      activeAdmins.some((user) => targetEmails.has(user.email)) && remaining.length === 0,
    includesSelf: targetEmails.has(currentEmail),
  };
}
