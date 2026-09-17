/**
 * Role x Function x Action permission matrix (F1-10 .. F1-12).
 *
 * Two rules that shape this model:
 * - Function and Action are FIXED SEED DATA, read-only. Only Role is created, renamed or deleted
 *   (PROTOTYPE_DEBT 1.2). Every Function must have a matching `@PreAuthorize` in the backend, so an
 *   admin cannot invent a Function and end up with a checkbox that guards nothing.
 * - STUDENT's basic learning rights (view/submit, review, interview, practise questions) do NOT go
 *   through this matrix at all — F1-05, layer one.
 *
 * The function list is F1-12 from 01-rd/req/identity.md verbatim: TEN functions. The static mockup
 * carries an eleventh, REJUDGE_MANAGEMENT, which DEC-2026-0828-remove-rejudge-scope removed
 * together with the whole rejudge feature.
 */
export type ActionKey = "create" | "read" | "update" | "delete";

export const ACTION_KEYS: ActionKey[] = ["create", "read", "update", "delete"];

export type FunctionKey =
  | "PROBLEM_AUTHORING"
  | "TESTCASE_MANAGEMENT"
  | "CLASS_MANAGEMENT"
  | "USER_MANAGEMENT"
  | "JUDGE_QUEUE_MONITOR"
  | "AI_CONFIG"
  | "AI_TOKEN_BUDGET"
  | "SYSTEM_AUDIT_LOG"
  | "INTERVIEW_BANK_MANAGEMENT"
  | "PERMISSION_MATRIX";

/** In F1-12 order. */
export const FUNCTION_KEYS: FunctionKey[] = [
  "PROBLEM_AUTHORING",
  "TESTCASE_MANAGEMENT",
  "CLASS_MANAGEMENT",
  "USER_MANAGEMENT",
  "JUDGE_QUEUE_MONITOR",
  "AI_CONFIG",
  "AI_TOKEN_BUDGET",
  "SYSTEM_AUDIT_LOG",
  "INTERVIEW_BANK_MANAGEMENT",
  "PERMISSION_MATRIX",
];

export type Role = {
  key: string;
  label: string;
  /** System roles (STUDENT/INSTRUCTOR/ADMIN) cannot be deleted. */
  system: boolean;
};

export type ActionGrant = Record<ActionKey, boolean>;

export type RolePermissions = Partial<Record<FunctionKey, ActionGrant>>;

export type PermissionMatrixPage = {
  roles: Role[];
  permissions: Record<string, RolePermissions>;
};

export const NO_GRANT: ActionGrant = {
  create: false,
  read: false,
  update: false,
  delete: false,
};

export function grantsFor(
  permissions: Record<string, RolePermissions>,
  roleKey: string,
  functionKey: FunctionKey,
): ActionGrant {
  return permissions[roleKey]?.[functionKey] ?? NO_GRANT;
}
