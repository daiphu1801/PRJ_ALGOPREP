// Real calls of the permission-matrix screen (03-dd/api/F1_identity.md #30 to #34, #9 for the acting
// admin's role). A role is addressed by its `code` on the screen (`Role.key`) and by its id on the
// wire; the catalogue ids of functions and actions travel with the page so a cell can be addressed.
import { api } from "@/shared/api";
import {
  ACTION_KEYS,
  FUNCTION_KEYS,
  type ActionKey,
  type BaseCategory,
  type FunctionKey,
  type PermissionMatrixPage,
  type Role,
  type RolePermissions,
} from "../model/types";

const RBAC = "/identity/rbac";

type RoleDto = {
  id: string;
  code: string;
  name: string;
  baseCategory: BaseCategory;
  isSystem: boolean;
  userCount?: number;
};

type CatalogDto = { id: string; code: string };

type MatrixDto = {
  functions: CatalogDto[];
  actions: CatalogDto[];
  cells: {
    roleId: string;
    functionId: string;
    actionId: string;
    granted: boolean;
  }[];
};

export function toRole(dto: RoleDto): Role {
  return {
    id: dto.id,
    key: dto.code,
    label: dto.name,
    system: dto.isSystem,
    baseCategory: dto.baseCategory,
    userCount: dto.userCount ?? 0,
  };
}

/** #30 and #32 for the grid, #9 for the role the acting admin holds. */
export async function realFetchPermissionMatrix(): Promise<PermissionMatrixPage> {
  const [matrix, roles, me] = await Promise.all([
    api.get<MatrixDto>(`${RBAC}/matrix`),
    api.get<{ items: RoleDto[] }>(`${RBAC}/roles`),
    api.get<{ roleName: string }>("/identity/me/profile"),
  ]);
  const functionById = new Map(matrix.functions.map((f) => [f.id, f.code]));
  const actionById = new Map(matrix.actions.map((a) => [a.id, a.code]));
  const codeByRoleId = new Map(roles.items.map((r) => [r.id, r.code]));

  const permissions: Record<string, RolePermissions> = {};
  for (const role of roles.items) {
    const grants: RolePermissions = {};
    for (const functionKey of FUNCTION_KEYS) {
      grants[functionKey] = {
        create: false,
        read: false,
        update: false,
        delete: false,
      };
    }
    permissions[role.code] = grants;
  }
  for (const cell of matrix.cells) {
    const roleCode = codeByRoleId.get(cell.roleId);
    const functionCode = functionById.get(cell.functionId) as
      FunctionKey | undefined;
    const action = actionById.get(cell.actionId)?.toLowerCase() as
      ActionKey | undefined;
    const grant =
      roleCode && functionCode ? permissions[roleCode]?.[functionCode] : null;
    if (grant && action && ACTION_KEYS.includes(action)) {
      grant[action] = cell.granted;
    }
  }

  const functionIds: Partial<Record<FunctionKey, string>> = {};
  for (const f of matrix.functions) {
    functionIds[f.code as FunctionKey] = f.id;
  }
  const actionIds = Object.fromEntries(
    matrix.actions.map((a) => [a.code.toLowerCase(), a.id]),
  ) as Record<ActionKey, string>;

  return {
    functionIds,
    actionIds,
    currentRoleKey:
      roles.items.find((role) => role.name === me.roleName)?.code ?? "",
    roles: roles.items.map(toRole),
    permissions,
  };
}

/** `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}` (#31): the answer is the stored value. */
export async function realToggleCell(input: {
  roleId: string;
  functionId: string;
  actionId: string;
  granted: boolean;
}): Promise<{ granted: boolean }> {
  const result = await api.patch<{ granted: boolean }>(
    `${RBAC}/matrix/${input.roleId}/${input.functionId}/${input.actionId}`,
    { granted: input.granted },
  );
  return { granted: result.granted };
}

/** `POST /rbac/roles` (#33): a custom role starts with no permission. */
export async function realCreateRole(input: {
  name: string;
  baseCategory: BaseCategory;
}): Promise<Role> {
  return toRole(await api.post<RoleDto>(`${RBAC}/roles`, input));
}

/** `DELETE /rbac/roles/{roleId}` (#34): 204. */
export async function realDeleteRole(input: { roleId: string }): Promise<void> {
  await api.delete(`${RBAC}/roles/${input.roleId}`);
}
