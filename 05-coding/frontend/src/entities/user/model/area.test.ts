import { describe, expect, it } from "vitest";
import { HOME_PATH_BY_ROLE, ROLE_BY_AREA } from "./area";

describe("ROLE_BY_AREA", () => {
  it("covers all three permission-gated areas", () => {
    expect(Object.keys(ROLE_BY_AREA).sort()).toEqual(["admin", "instructor", "student"]);
  });
});

describe("HOME_PATH_BY_ROLE", () => {
  it("gives each role exactly one destination, and INSTRUCTOR/ADMIN land in their own area", () => {
    expect(HOME_PATH_BY_ROLE.STUDENT).toBe("/progress");
    expect(HOME_PATH_BY_ROLE.INSTRUCTOR.startsWith("/instructor/")).toBe(true);
    // Pins the rule: ADMIN's destination must stay under /admin — if it ever points elsewhere,
    // the prefix-based middleware guard stops protecting that route.
    expect(HOME_PATH_BY_ROLE.ADMIN.startsWith("/admin/")).toBe(true);
  });
});
