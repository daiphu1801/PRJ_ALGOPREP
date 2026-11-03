import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const patch = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realCreateRole,
  realDeleteRole,
  realFetchPermissionMatrix,
  realToggleCell,
} from "./real-permission-matrix";

const RBAC = "/identity/rbac";

const ROLES = [
  {
    id: "r1",
    code: "STUDENT",
    name: "Người học",
    baseCategory: "STUDENT",
    isSystem: true,
    userCount: 5,
  },
  {
    id: "r2",
    code: "INSTRUCTOR",
    name: "Giảng viên",
    baseCategory: "INSTRUCTOR",
    isSystem: true,
    userCount: 2,
  },
  {
    id: "r3",
    code: "ADMIN",
    name: "Quản trị",
    baseCategory: "ADMIN",
    isSystem: true,
    userCount: 1,
  },
  {
    id: "r4",
    code: "role_tro_giang",
    name: "Trợ giảng",
    baseCategory: "INSTRUCTOR",
    isSystem: false,
    userCount: 0,
  },
];

describe("realFetchPermissionMatrix (#30, #32, #9)", () => {
  beforeEach(() => {
    get.mockReset();
    get.mockImplementation((path: string) => {
      if (path === `${RBAC}/matrix`) {
        return Promise.resolve({
          roles: ROLES,
          functions: [
            { id: "f-user", code: "USER_MANAGEMENT" },
            { id: "f-matrix", code: "PERMISSION_MATRIX" },
            { id: "f-new", code: "FUTURE_FUNCTION" },
          ],
          actions: [
            { id: "a-c", code: "CREATE" },
            { id: "a-r", code: "READ" },
            { id: "a-u", code: "UPDATE" },
            { id: "a-d", code: "DELETE" },
          ],
          cells: [
            {
              roleId: "r3",
              functionId: "f-matrix",
              actionId: "a-u",
              granted: true,
            },
            {
              roleId: "r3",
              functionId: "f-user",
              actionId: "a-r",
              granted: true,
            },
            {
              roleId: "r2",
              functionId: "f-user",
              actionId: "a-r",
              granted: false,
            },
            {
              roleId: "r4",
              functionId: "f-new",
              actionId: "a-r",
              granted: true,
            },
            {
              roleId: "unknown",
              functionId: "f-user",
              actionId: "a-c",
              granted: true,
            },
          ],
        });
      }
      if (path === `${RBAC}/roles`) return Promise.resolve({ items: ROLES });
      return Promise.resolve({ roleName: "Quản trị" });
    });
  });

  it("maps roles, the grid and the ids needed to address a cell", async () => {
    const page = await realFetchPermissionMatrix();

    expect(
      page.roles.map((r) => [r.id, r.key, r.label, r.system, r.userCount]),
    ).toEqual([
      ["r1", "STUDENT", "Người học", true, 5],
      ["r2", "INSTRUCTOR", "Giảng viên", true, 2],
      ["r3", "ADMIN", "Quản trị", true, 1],
      ["r4", "role_tro_giang", "Trợ giảng", false, 0],
    ]);
    expect(page.permissions.ADMIN?.PERMISSION_MATRIX).toEqual({
      create: false,
      read: false,
      update: true,
      delete: false,
    });
    expect(page.permissions.ADMIN?.USER_MANAGEMENT?.read).toBe(true);
    expect(page.permissions.INSTRUCTOR?.USER_MANAGEMENT?.read).toBe(false);
    // A role with no stored cell reads as every cell off; an unknown function or role is ignored.
    expect(page.permissions.STUDENT?.USER_MANAGEMENT).toEqual({
      create: false,
      read: false,
      update: false,
      delete: false,
    });
    expect(page.permissions.role_tro_giang).not.toHaveProperty(
      "FUTURE_FUNCTION",
    );
    expect(page.functionIds).toMatchObject({
      USER_MANAGEMENT: "f-user",
      PERMISSION_MATRIX: "f-matrix",
    });
    expect(page.actionIds).toEqual({
      create: "a-c",
      read: "a-r",
      update: "a-u",
      delete: "a-d",
    });
  });

  it("finds the acting admin's role by the role name in the profile", async () => {
    expect((await realFetchPermissionMatrix()).currentRoleKey).toBe("ADMIN");

    get.mockImplementation((path: string) =>
      Promise.resolve(
        path === `${RBAC}/matrix`
          ? { roles: [], functions: [], actions: [], cells: [] }
          : path === `${RBAC}/roles`
            ? { items: ROLES }
            : { roleName: "Không có vai trò này" },
      ),
    );
    expect((await realFetchPermissionMatrix()).currentRoleKey).toBe("");
  });

  it("rethrows a failure of any of the three calls", async () => {
    get.mockImplementation((path: string) =>
      path === `${RBAC}/roles`
        ? Promise.reject(new ApiError("IDT-445", 403))
        : Promise.resolve({
            roles: [],
            functions: [],
            actions: [],
            cells: [],
            roleName: "",
          }),
    );
    await expect(realFetchPermissionMatrix()).rejects.toMatchObject({
      code: "IDT-445",
    });
  });
});

describe("matrix and role actions (#31, #33, #34)", () => {
  beforeEach(() => {
    patch.mockReset();
    post.mockReset();
    del.mockReset();
  });

  it("toggles a cell by ids and returns the stored value", async () => {
    patch.mockResolvedValue({ granted: true, changed: true });
    expect(
      await realToggleCell({
        roleId: "r2",
        functionId: "f-user",
        actionId: "a-c",
        granted: true,
      }),
    ).toEqual({ granted: true });
    expect(patch).toHaveBeenCalledWith(`${RBAC}/matrix/r2/f-user/a-c`, {
      granted: true,
    });
  });

  it("creates a role with the name and category only and maps the answer", async () => {
    post.mockResolvedValue({
      id: "new",
      code: "role_ops",
      name: "Ops",
      baseCategory: "ADMIN",
      isSystem: false,
      userCount: 0,
    });
    expect(
      await realCreateRole({ name: "Ops", baseCategory: "ADMIN" }),
    ).toEqual({
      id: "new",
      key: "role_ops",
      label: "Ops",
      system: false,
      baseCategory: "ADMIN",
      userCount: 0,
    });
    expect(post).toHaveBeenCalledWith(`${RBAC}/roles`, {
      name: "Ops",
      baseCategory: "ADMIN",
    });
  });

  it("deletes a role by id", async () => {
    del.mockResolvedValue(undefined);
    await realDeleteRole({ roleId: "r4" });
    expect(del).toHaveBeenCalledWith(`${RBAC}/roles/r4`);
  });

  it("rethrows the backend error code of every action", async () => {
    for (const [code, run] of [
      [
        "IDT-420",
        () =>
          realToggleCell({
            roleId: "a",
            functionId: "b",
            actionId: "c",
            granted: false,
          }),
      ],
      [
        "IDT-423",
        () =>
          realToggleCell({
            roleId: "a",
            functionId: "b",
            actionId: "c",
            granted: true,
          }),
      ],
      ["IDT-425", () => realCreateRole({ name: "X", baseCategory: "STUDENT" })],
      ["IDT-V22", () => realCreateRole({ name: " ", baseCategory: "STUDENT" })],
      ["IDT-422", () => realDeleteRole({ roleId: "a" })],
    ] as const) {
      const failure = new ApiError(code, 422);
      patch.mockRejectedValue(failure);
      post.mockRejectedValue(failure);
      del.mockRejectedValue(failure);
      await expect(run(), code).rejects.toMatchObject({ code });
    }
  });
});
