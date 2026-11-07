import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const patch = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realChangeUserRole,
  realCreateUserAccount,
  realFetchAdminUserPage,
  realLockUser,
  realResetUserPassword,
  realUnlockUser,
} from "./real-user-management";

const USERS = "/identity/admin/users";
const ROLES = {
  items: [
    { id: "r-student", code: "STUDENT", isSystem: true },
    { id: "r-instructor", code: "INSTRUCTOR", isSystem: true },
    { id: "r-admin", code: "ADMIN", isSystem: true },
    { id: "r-custom", code: "STUDENT", isSystem: false },
  ],
};

describe("realFetchAdminUserPage (#35, #62, #63, #9)", () => {
  beforeEach(() => {
    get.mockReset();
    get.mockImplementation((path: string) => {
      if (path === USERS) {
        return Promise.resolve({
          items: [
            {
              id: "u1",
              displayName: "An",
              email: "an@x.test",
              baseCategory: "STUDENT",
              status: "ACTIVE",
              lastActiveAt: "2026-11-02T05:00:00Z",
              hasPassword: true,
              solvedCount: 3,
              totalSubmissions: 12,
            },
            {
              id: "u2",
              displayName: "Binh",
              email: "binh@x.test",
              baseCategory: "ADMIN",
              status: "PENDING_VERIFICATION",
              lastActiveAt: null,
              hasPassword: false,
              solvedCount: 0,
              totalSubmissions: 0,
            },
            {
              id: "u3",
              displayName: "Chi",
              email: "chi@x.test",
              baseCategory: "INSTRUCTOR",
              status: "DEACTIVATED",
              lastActiveAt: "2026-10-01T00:00:00Z",
              hasPassword: true,
              solvedCount: 1,
              totalSubmissions: 5,
            },
          ],
          page: 2,
          pageSize: 20,
          total: 41,
        });
      }
      if (path === `${USERS}/management-stats`) {
        return Promise.resolve({ pendingVerificationCount: 7 });
      }
      if (path === `${USERS}/role-distribution`) {
        return Promise.resolve({
          items: [
            { groupCode: "STUDENT", count: 1196, percent: 93.1 },
            { groupCode: "INSTRUCTOR", count: 74, percent: 5.8 },
            { groupCode: "ADMIN", count: 9, percent: 0.7 },
            { groupCode: "DEACTIVATED", count: 5, percent: 0.4 },
          ],
        });
      }
      return Promise.resolve({ email: "admin@x.test" });
    });
  });

  it("asks the server for the filters and the page", async () => {
    await realFetchAdminUserPage({
      status: "locked",
      role: "instructor",
      query: "  an  ",
      page: 3,
    });
    expect(get).toHaveBeenCalledWith(USERS, {
      status: "DEACTIVATED",
      role: "INSTRUCTOR",
      q: "an",
      page: 3,
      pageSize: 20,
    });
  });

  it("leaves every filter out when none is chosen", async () => {
    await realFetchAdminUserPage({
      status: "all",
      role: "all",
      query: "   ",
      page: 1,
    });
    expect(get).toHaveBeenCalledWith(USERS, {
      status: undefined,
      role: undefined,
      q: undefined,
      page: 1,
      pageSize: 20,
    });
  });

  it("maps rows, counters, the acting admin and the solved and submission counts of the server", async () => {
    const page = await realFetchAdminUserPage({
      status: "all",
      role: "all",
      query: "",
      page: 2,
    });

    expect(page.currentUserEmail).toBe("admin@x.test");
    expect(page.users).toEqual([
      {
        id: "u1",
        name: "An",
        email: "an@x.test",
        role: "student",
        status: "active",
        lastActiveAt: "2026-11-02T05:00:00Z",
        hasPassword: true,
        solvedCount: 3,
        submissionCount: 12,
      },
      {
        id: "u2",
        name: "Binh",
        email: "binh@x.test",
        role: "admin",
        status: "pending",
        lastActiveAt: null,
        hasPassword: false,
        solvedCount: 0,
        submissionCount: 0,
      },
      {
        id: "u3",
        name: "Chi",
        email: "chi@x.test",
        role: "instructor",
        status: "locked",
        lastActiveAt: "2026-10-01T00:00:00Z",
        hasPassword: true,
        solvedCount: 1,
        submissionCount: 5,
      },
    ]);
    expect(page.users.map((u) => [u.solvedCount, u.submissionCount])).toEqual([
      [3, 12],
      [0, 0],
      [1, 5],
    ]);
    expect(page.pendingTasks).toEqual([
      { key: "pendingEmail", count: 7, colorVar: "--color-admin-warn" },
    ]);
    expect(page.roleDistribution.map((item) => item.role)).toEqual([
      "student",
      "instructor",
      "admin",
      "deactivated",
    ]);
    expect(page.roleDistribution[0]).toEqual({
      role: "student",
      label: "1.196 · 93,1%",
      percent: 93.1,
    });
    expect(page.totalUsers).toBe(41);
    expect(page.totalPages).toBe(3);
  });

  it("reports one page when the list is empty", async () => {
    get.mockImplementation((path: string) =>
      Promise.resolve(
        path === USERS
          ? { items: [], page: 1, pageSize: 20, total: 0 }
          : path.endsWith("management-stats")
            ? { pendingVerificationCount: 0 }
            : path.endsWith("role-distribution")
              ? { items: [] }
              : { email: "a@x.test" },
      ),
    );
    const page = await realFetchAdminUserPage({
      status: "all",
      role: "all",
      query: "",
      page: 1,
    });
    expect(page.totalPages).toBe(1);
  });

  it("rethrows a failure of any of the four calls", async () => {
    get.mockImplementation((path: string) =>
      path.endsWith("role-distribution")
        ? Promise.reject(new ApiError("IDT-445", 403))
        : Promise.resolve({ items: [], total: 0, pageSize: 20 }),
    );
    await expect(
      realFetchAdminUserPage({
        status: "all",
        role: "all",
        query: "",
        page: 1,
      }),
    ).rejects.toMatchObject({ code: "IDT-445" });
  });
});

describe("user actions (#36, #37, #38, #50)", () => {
  beforeEach(() => {
    get.mockReset();
    post.mockReset();
    patch.mockReset();
    get.mockResolvedValue(ROLES);
    patch.mockResolvedValue({});
    post.mockResolvedValue({});
  });

  it("changes a role with the id of the built-in role and no flag for another account", async () => {
    await realChangeUserRole({ id: "u1", role: "instructor", self: false });
    expect(get).toHaveBeenCalledWith("/identity/rbac/roles");
    expect(patch).toHaveBeenCalledWith(`${USERS}/u1/role`, {
      targetRoleId: "r-instructor",
    });
  });

  it("acknowledges the effect when the admin changes their own role", async () => {
    await realChangeUserRole({ id: "me", role: "student", self: true });
    expect(patch).toHaveBeenCalledWith(`${USERS}/me/role`, {
      targetRoleId: "r-student",
      acknowledgeSelfImpact: true,
    });
  });

  it("fails loudly when a built-in role is missing", async () => {
    get.mockResolvedValue({ items: [] });
    await expect(
      realChangeUserRole({ id: "u1", role: "admin", self: false }),
    ).rejects.toThrow("System role admin");
    expect(patch).not.toHaveBeenCalled();
  });

  it("locks with a trimmed reason, the e-mail switch and the acknowledgement only for self", async () => {
    await realLockUser({
      id: "u1",
      reason: "  Spam  ",
      notify: true,
      self: false,
    });
    expect(patch).toHaveBeenLastCalledWith(`${USERS}/u1/status`, {
      targetStatus: "DEACTIVATED",
      reason: "Spam",
      notifyByEmail: true,
    });
    await realLockUser({ id: "me", reason: "", notify: false, self: true });
    expect(patch).toHaveBeenLastCalledWith(`${USERS}/me/status`, {
      targetStatus: "DEACTIVATED",
      notifyByEmail: false,
      acknowledgeSelfImpact: true,
    });
  });

  it("unlocks with the target status only", async () => {
    await realUnlockUser({ id: "u3" });
    expect(patch).toHaveBeenCalledWith(`${USERS}/u3/status`, {
      targetStatus: "ACTIVE",
    });
  });

  it("asks for a temporary password without a body", async () => {
    await realResetUserPassword({ id: "u1" });
    expect(post).toHaveBeenCalledWith(`${USERS}/u1/reset-password`);
  });

  it("creates an account with the built-in role id and maps the answer", async () => {
    post.mockResolvedValue({
      id: "new",
      displayName: "Moi",
      email: "moi@x.test",
      baseCategory: "INSTRUCTOR",
      status: "PENDING_VERIFICATION",
      lastActiveAt: null,
      hasPassword: true,
    });

    const created = await realCreateUserAccount({
      name: "Moi",
      email: "moi@x.test",
      role: "instructor",
    });

    expect(post).toHaveBeenCalledWith(USERS, {
      displayName: "Moi",
      email: "moi@x.test",
      roleId: "r-instructor",
    });
    expect(created).toMatchObject({
      id: "new",
      role: "instructor",
      status: "pending",
    });
  });

  it("rethrows the backend error code of every action", async () => {
    for (const [code, run] of [
      [
        "IDT-432",
        () => realChangeUserRole({ id: "a", role: "student", self: false }),
      ],
      [
        "IDT-V21",
        () => realLockUser({ id: "a", reason: "", notify: false, self: true }),
      ],
      ["IDT-448", () => realUnlockUser({ id: "a" })],
      ["IDT-450", () => realResetUserPassword({ id: "a" })],
      [
        "IDT-440",
        () =>
          realCreateUserAccount({
            name: "N",
            email: "a@x.test",
            role: "student",
          }),
      ],
    ] as const) {
      const failure = new ApiError(code, 422);
      patch.mockRejectedValue(failure);
      post.mockRejectedValue(failure);
      await expect(run(), code).rejects.toMatchObject({ code });
    }
  });
});
