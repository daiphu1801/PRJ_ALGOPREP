import { describe, expect, it } from "vitest";
import { checkLock, checkRoleChange, partitionByStatus } from "./guards";
import type { AdminUser } from "./types";

const base = { solvedCount: 0, submissionCount: 0, lastActiveLabel: "-" };
const user = (
  email: string,
  role: AdminUser["role"],
  status: AdminUser["status"] = "active",
): AdminUser => ({
  ...base,
  name: email,
  email,
  role,
  status,
});

describe("checkLock", () => {
  it("blocks locking the only active admin", () => {
    const users = [user("a@x", "admin"), user("s@x", "student")];
    expect(checkLock(users, new Set(["a@x"]), "a@x")).toEqual({
      blockedLastAdmin: true,
      includesSelf: true,
    });
  });

  it("allows locking an admin while another active admin remains, but warns about self", () => {
    const users = [user("a@x", "admin"), user("b@x", "admin")];
    expect(checkLock(users, new Set(["a@x"]), "a@x")).toEqual({
      blockedLastAdmin: false,
      includesSelf: true,
    });
  });

  it("a locked admin does not count as remaining", () => {
    const users = [user("a@x", "admin"), user("b@x", "admin", "locked")];
    expect(checkLock(users, new Set(["a@x"]), "z@x").blockedLastAdmin).toBe(
      true,
    );
  });

  it("locking only students is unrestricted", () => {
    const users = [user("a@x", "admin"), user("s@x", "student")];
    expect(checkLock(users, new Set(["s@x"]), "a@x")).toEqual({
      blockedLastAdmin: false,
      includesSelf: false,
    });
  });
});

describe("partitionByStatus", () => {
  const users = [
    user("a@x", "student"),
    user("b@x", "student", "locked"),
    user("c@x", "student", "pending"),
  ];

  it("splits a mixed selection so each button acts only on rows it can change", () => {
    const { lockable, unlockable } = partitionByStatus(
      users,
      new Set(["a@x", "b@x"]),
    );
    expect(lockable.map((row) => row.email)).toEqual(["a@x"]);
    expect(unlockable.map((row) => row.email)).toEqual(["b@x"]);
  });

  it("counts an unverified account as lockable — it still has access to lose", () => {
    const { lockable, unlockable } = partitionByStatus(users, new Set(["c@x"]));
    expect(lockable).toHaveLength(1);
    expect(unlockable).toHaveLength(0);
  });

  it("returns nothing to do for a selection of only locked rows", () => {
    const { lockable, unlockable } = partitionByStatus(users, new Set(["b@x"]));
    expect(lockable).toHaveLength(0);
    expect(unlockable).toHaveLength(1);
  });
});

describe("checkRoleChange", () => {
  it("blocks demoting the only active admin", () => {
    const users = [user("a@x", "admin"), user("s@x", "student")];
    expect(
      checkRoleChange(users, "a@x", "instructor", "z@x").blockedLastAdmin,
    ).toBe(true);
  });

  it("warns, but allows, when an admin demotes themselves while another admin remains", () => {
    const users = [user("a@x", "admin"), user("b@x", "admin")];
    expect(checkRoleChange(users, "a@x", "student", "a@x")).toEqual({
      blockedLastAdmin: false,
      includesSelf: true,
    });
  });

  it("promoting or moving between non-admin roles is unrestricted", () => {
    const users = [user("a@x", "admin"), user("s@x", "student")];
    expect(checkRoleChange(users, "s@x", "admin", "a@x")).toEqual({
      blockedLastAdmin: false,
      includesSelf: false,
    });
    expect(
      checkRoleChange(users, "s@x", "instructor", "a@x").blockedLastAdmin,
    ).toBe(false);
  });
});
