// Real calls of the user-management screen (03-dd/api/F1_identity.md #35 to #38, #50, #62, #63,
// #32 for role ids, #9 for the acting admin's e-mail). Backend task 11, section 0.6 "Triển khai
// 2026-11-02" and "Triển khai 2026-11-07". Each row carries `lastActiveAt`, `solvedCount` and
// `totalSubmissions` (read from the F1 read models, 0 when the account has no row).
import { api } from "@/shared/api";
import type {
  AdminUser,
  AdminUserPage,
  AdminUserRole,
  AdminUserStatus,
  RoleDistributionItem,
} from "../model/types";

export const ADMIN_USERS_PAGE_SIZE = 20;

const USERS_PATH = "/identity/admin/users";

/** What the screen asks of `GET /admin/users`; the server filters and pages. */
export type AdminUserListParams = {
  status: AdminUserStatus | "all";
  role: AdminUserRole | "all";
  query: string;
  page: number;
};

type UserDto = {
  id: string;
  displayName: string;
  email: string;
  baseCategory: "STUDENT" | "INSTRUCTOR" | "ADMIN";
  status: "ACTIVE" | "PENDING_VERIFICATION" | "DEACTIVATED";
  lastActiveAt: string | null;
  hasPassword: boolean;
  solvedCount: number;
  totalSubmissions: number;
};

type UserListDto = {
  items: UserDto[];
  page: number;
  pageSize: number;
  total: number;
};

type DistributionDto = {
  items: { groupCode: string; count: number; percent: number }[];
};

const STATUS_OF: Record<UserDto["status"], AdminUserStatus> = {
  ACTIVE: "active",
  PENDING_VERIFICATION: "pending",
  DEACTIVATED: "locked",
};

const STATUS_PARAM: Record<AdminUserStatus, UserDto["status"]> = {
  active: "ACTIVE",
  pending: "PENDING_VERIFICATION",
  locked: "DEACTIVATED",
};

const ROLE_OF: Record<UserDto["baseCategory"], AdminUserRole> = {
  STUDENT: "student",
  INSTRUCTOR: "instructor",
  ADMIN: "admin",
};

export function toAdminUser(dto: UserDto): AdminUser {
  return {
    id: dto.id,
    name: dto.displayName,
    email: dto.email,
    role: ROLE_OF[dto.baseCategory],
    status: STATUS_OF[dto.status],
    lastActiveAt: dto.lastActiveAt,
    hasPassword: dto.hasPassword,
    solvedCount: dto.solvedCount,
    submissionCount: dto.totalSubmissions,
  };
}

function toDistribution(dto: DistributionDto): RoleDistributionItem[] {
  return dto.items.map((item) => {
    const role =
      item.groupCode === "DEACTIVATED"
        ? "deactivated"
        : ROLE_OF[item.groupCode as UserDto["baseCategory"]];
    return {
      role,
      label: `${item.count.toLocaleString("vi-VN")} · ${item.percent.toLocaleString("vi-VN")}%`,
      percent: item.percent,
    };
  });
}

/** List, the two counters and the acting admin's e-mail, fetched together (#35, #62, #63, #9). */
export async function realFetchAdminUserPage(
  params: AdminUserListParams,
): Promise<AdminUserPage> {
  const [list, stats, distribution, me] = await Promise.all([
    api.get<UserListDto>(USERS_PATH, {
      status: params.status === "all" ? undefined : STATUS_PARAM[params.status],
      role: params.role === "all" ? undefined : params.role.toUpperCase(),
      q: params.query.trim() || undefined,
      page: params.page,
      pageSize: ADMIN_USERS_PAGE_SIZE,
    }),
    api.get<{ pendingVerificationCount: number }>(
      `${USERS_PATH}/management-stats`,
    ),
    api.get<DistributionDto>(`${USERS_PATH}/role-distribution`),
    api.get<{ email: string }>("/identity/me/profile"),
  ]);
  return {
    currentUserEmail: me.email,
    users: list.items.map(toAdminUser),
    roleDistribution: toDistribution(distribution),
    pendingTasks: [
      {
        key: "pendingEmail",
        count: stats.pendingVerificationCount,
        colorVar: "--color-admin-warn",
      },
    ],
    totalUsers: list.total,
    totalPages: Math.max(1, Math.ceil(list.total / list.pageSize)),
  };
}

type RoleDto = { id: string; code: string; isSystem: boolean };

/** Id of the built-in role behind a screen role; the screen only knows the three base roles. */
async function systemRoleId(role: AdminUserRole): Promise<string> {
  const { items } = await api.get<{ items: RoleDto[] }>("/identity/rbac/roles");
  const found = items.find((r) => r.isSystem && r.code === role.toUpperCase());
  if (!found) {
    throw new Error(`System role ${role} is not available`);
  }
  return found.id;
}

/** `PATCH /admin/users/{id}/role` (#36). `self` sends the acknowledgement the dialog already asked for. */
export async function realChangeUserRole(input: {
  id: string;
  role: AdminUserRole;
  self: boolean;
}): Promise<void> {
  await api.patch(`${USERS_PATH}/${input.id}/role`, {
    targetRoleId: await systemRoleId(input.role),
    ...(input.self ? { acknowledgeSelfImpact: true } : {}),
  });
}

/** `PATCH /admin/users/{id}/status` (#37). Only a lock carries a reason and the e-mail switch. */
export async function realLockUser(input: {
  id: string;
  reason: string;
  notify: boolean;
  self: boolean;
}): Promise<void> {
  await api.patch(`${USERS_PATH}/${input.id}/status`, {
    targetStatus: "DEACTIVATED",
    ...(input.reason.trim() ? { reason: input.reason.trim() } : {}),
    notifyByEmail: input.notify,
    ...(input.self ? { acknowledgeSelfImpact: true } : {}),
  });
}

export async function realUnlockUser(input: { id: string }): Promise<void> {
  await api.patch(`${USERS_PATH}/${input.id}/status`, {
    targetStatus: "ACTIVE",
  });
}

/** `POST /admin/users/{id}/reset-password` (#38): 204, the temporary password only goes by e-mail. */
export async function realResetUserPassword(input: {
  id: string;
}): Promise<void> {
  await api.post(`${USERS_PATH}/${input.id}/reset-password`);
}

/** `POST /admin/users` (#50): the screen role is turned into the id of the built-in role. */
export async function realCreateUserAccount(input: {
  name: string;
  email: string;
  role: Exclude<AdminUserRole, "admin">;
}): Promise<AdminUser> {
  const dto = await api.post<UserDto>(USERS_PATH, {
    displayName: input.name,
    email: input.email,
    roleId: await systemRoleId(input.role),
  });
  return toAdminUser(dto);
}
