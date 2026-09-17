// PROTOTYPE mock — no backend endpoint exists yet.
// From 09-layoutBase/Admin - Ma trận phân quyền.dc.html:249-284, minus REJUDGE_MANAGEMENT
// (DEC-2026-0828-remove-rejudge-scope). STUDENT deliberately has no entry: its basic learning
// rights sit outside the matrix (F1-05), which is why the screen renders its cells read-only.
import type { ActionGrant, PermissionMatrixPage, Role, RolePermissions } from "../../model/types";

const ALL: ActionGrant = { create: true, read: true, update: true, delete: true };
const NONE: ActionGrant = { create: false, read: false, update: false, delete: false };

const ROLES: Role[] = [
  { key: "STUDENT", label: "STUDENT", system: true },
  { key: "INSTRUCTOR", label: "INSTRUCTOR", system: true },
  { key: "ADMIN", label: "ADMIN", system: true },
];

const INSTRUCTOR: RolePermissions = {
  PROBLEM_AUTHORING: { ...ALL },
  TESTCASE_MANAGEMENT: { ...ALL },
  CLASS_MANAGEMENT: { ...ALL },
  INTERVIEW_BANK_MANAGEMENT: { ...ALL },
  USER_MANAGEMENT: { ...NONE },
  JUDGE_QUEUE_MONITOR: { ...NONE },
  AI_CONFIG: { ...NONE },
  AI_TOKEN_BUDGET: { ...NONE },
  SYSTEM_AUDIT_LOG: { ...NONE },
  PERMISSION_MATRIX: { ...NONE },
};

const ADMIN: RolePermissions = {
  PROBLEM_AUTHORING: { ...ALL },
  TESTCASE_MANAGEMENT: { ...ALL },
  CLASS_MANAGEMENT: { ...ALL },
  INTERVIEW_BANK_MANAGEMENT: { ...ALL },
  USER_MANAGEMENT: { ...ALL },
  JUDGE_QUEUE_MONITOR: { ...ALL },
  AI_CONFIG: { ...ALL },
  AI_TOKEN_BUDGET: { ...ALL },
  SYSTEM_AUDIT_LOG: { ...ALL },
  PERMISSION_MATRIX: { ...ALL },
};

export function fetchPermissionMatrix(): PermissionMatrixPage {
  return {
    roles: ROLES.map((role) => ({ ...role })),
    permissions: {
      INSTRUCTOR: structuredClone(INSTRUCTOR),
      ADMIN: structuredClone(ADMIN),
    },
  };
}
