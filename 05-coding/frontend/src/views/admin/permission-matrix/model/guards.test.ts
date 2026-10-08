import { describe, expect, it } from "vitest";
import { checkCellToggle, validateRoleName } from "./guards";
import type { PermissionMatrixPage } from "./types";

const ALL = { create: true, read: true, update: true, delete: true };
const page: PermissionMatrixPage = {
  currentRoleKey: "ADMIN",
  roles: [
    {
      key: "INSTRUCTOR",
      label: "INSTRUCTOR",
      system: true,
      baseCategory: "INSTRUCTOR",
      userCount: 0,
    },
    {
      key: "ADMIN",
      label: "ADMIN",
      system: true,
      baseCategory: "ADMIN",
      userCount: 0,
    },
  ],
  permissions: { ADMIN: { PERMISSION_MATRIX: { ...ALL } }, INSTRUCTOR: {} },
};

describe("validateRoleName", () => {
  it("rejects empty, over-long and duplicate names", () => {
    expect(validateRoleName("   ", page.roles)).toBe("empty");
    expect(validateRoleName("x".repeat(51), page.roles)).toBe("tooLong");
    expect(validateRoleName("admin", page.roles)).toBe("duplicate");
    expect(validateRoleName("Trợ  giảng", page.roles)).toBeUndefined();
  });

  it("treats names that normalise to the same code as duplicates", () => {
    const roles = [
      ...page.roles,
      {
        key: "role_trợ_giảng",
        label: "Trợ giảng",
        system: false,
        baseCategory: "INSTRUCTOR" as const,
        userCount: 0,
      },
    ];
    expect(validateRoleName("trợ   giảng", roles)).toBe("duplicate");
  });
});

describe("checkCellToggle", () => {
  it("blocks removing the last ADMIN-category PERMISSION_MATRIX:UPDATE", () => {
    const check = checkCellToggle(
      page,
      "ADMIN",
      "PERMISSION_MATRIX",
      "update",
      true,
    );
    expect(check.blockedLastMatrixAdmin).toBe(true);
  });

  it("allows it while another ADMIN-category role still holds the cell", () => {
    const two: PermissionMatrixPage = {
      ...page,
      roles: [
        ...page.roles,
        {
          key: "role_ops",
          label: "Ops",
          system: false,
          baseCategory: "ADMIN" as const,
          userCount: 0,
        },
      ],
      permissions: {
        ...page.permissions,
        role_ops: { PERMISSION_MATRIX: { ...ALL } },
      },
    };
    expect(
      checkCellToggle(two, "ADMIN", "PERMISSION_MATRIX", "update", true)
        .blockedLastMatrixAdmin,
    ).toBe(false);
  });

  it("asks for confirmation when the acting admin revokes their own role, but not when granting", () => {
    expect(
      checkCellToggle(page, "ADMIN", "AI_CONFIG", "read", true).revokesOwnRole,
    ).toBe(true);
    expect(
      checkCellToggle(page, "ADMIN", "AI_CONFIG", "read", false).revokesOwnRole,
    ).toBe(false);
    expect(
      checkCellToggle(page, "INSTRUCTOR", "AI_CONFIG", "read", true)
        .revokesOwnRole,
    ).toBe(false);
  });
});
