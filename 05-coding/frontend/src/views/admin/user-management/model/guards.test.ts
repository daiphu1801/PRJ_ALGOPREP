import { describe, expect, it } from "vitest";
import { checkLock } from "./guards";
import type { AdminUser } from "./types";

const base = { solvedCount: 0, submissionCount: 0, lastActiveLabel: "-" };
const user = (email: string, role: AdminUser["role"], status: AdminUser["status"] = "active"): AdminUser => ({
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
    expect(checkLock(users, new Set(["a@x"]), "z@x").blockedLastAdmin).toBe(true);
  });

  it("locking only students is unrestricted", () => {
    const users = [user("a@x", "admin"), user("s@x", "student")];
    expect(checkLock(users, new Set(["s@x"]), "a@x")).toEqual({
      blockedLastAdmin: false,
      includesSelf: false,
    });
  });
});
