import type { ActionKey, FunctionKey, PermissionMatrixPage } from "./types";

export const ROLE_NAME_MAX = 50;

export type RoleNameError = "empty" | "tooLong" | "duplicate";

/** Code a custom role gets from its name. Two names that normalise to the same code collide. */
export function roleKeyFor(name: string): string {
  return `role_${name.trim().toLowerCase().replace(/\s+/g, "_")}`;
}

/**
 * BD ADM0202 Sheet 9 NO 3-4: required, at most 50 characters (a `[Suy luận]` length), and unique by
 * the code it generates. Compared against the label too, so "admin" cannot shadow the system ADMIN.
 */
export function validateRoleName(
  name: string,
  roles: PermissionMatrixPage["roles"],
): RoleNameError | undefined {
  const trimmed = name.trim();
  if (!trimmed) return "empty";
  if (trimmed.length > ROLE_NAME_MAX) return "tooLong";
  const key = roleKeyFor(trimmed);
  const taken = roles.some(
    (role) =>
      role.key.toLowerCase() === key ||
      role.label.toLowerCase() === trimmed.toLowerCase(),
  );
  return taken ? "duplicate" : undefined;
}

export type ToggleCheck = {
  /** Would leave no ADMIN-category role holding PERMISSION_MATRIX:UPDATE: hard block (Sheet 9 NO 9). */
  blockedLastMatrixAdmin: boolean;
  /** Turning off a cell of the role the acting admin holds: allowed after a confirmation (NO 8). */
  revokesOwnRole: boolean;
};

/** Rule check for flipping one cell. Only a cell that is currently ON can trip either rule. */
export function checkCellToggle(
  page: PermissionMatrixPage,
  roleKey: string,
  functionKey: FunctionKey,
  action: ActionKey,
  currentlyGranted: boolean,
): ToggleCheck {
  if (!currentlyGranted)
    return { blockedLastMatrixAdmin: false, revokesOwnRole: false };

  const role = page.roles.find((candidate) => candidate.key === roleKey);
  const isMatrixUpdate =
    functionKey === "PERMISSION_MATRIX" && action === "update";
  const otherHolders = page.roles.filter(
    (candidate) =>
      candidate.key !== roleKey &&
      candidate.baseCategory === "ADMIN" &&
      page.permissions[candidate.key]?.PERMISSION_MATRIX?.update,
  );
  return {
    blockedLastMatrixAdmin:
      isMatrixUpdate &&
      role?.baseCategory === "ADMIN" &&
      otherHolders.length === 0,
    revokesOwnRole: roleKey === page.currentRoleKey,
  };
}
