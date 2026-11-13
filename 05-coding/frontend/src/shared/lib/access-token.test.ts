import { describe, expect, it } from "vitest";
import { readBaseCategory } from "./access-token";

function tokenWith(claims: unknown): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `${encode({ alg: "RS256" })}.${encode(claims)}.signature`;
}

describe("readBaseCategory", () => {
  it("reads each valid category", () => {
    for (const category of ["STUDENT", "INSTRUCTOR", "ADMIN"] as const) {
      expect(readBaseCategory(tokenWith({ base_category: category }))).toBe(
        category,
      );
    }
  });

  it("returns null for no token, a malformed token or an unknown category", () => {
    expect(readBaseCategory(null)).toBeNull();
    expect(readBaseCategory("")).toBeNull();
    expect(readBaseCategory("not-a-jwt")).toBeNull();
    expect(readBaseCategory("a.%%%.c")).toBeNull();
    expect(readBaseCategory(tokenWith({ base_category: "OWNER" }))).toBeNull();
    expect(readBaseCategory(tokenWith({ sub: "x" }))).toBeNull();
    expect(readBaseCategory(tokenWith("string-payload"))).toBeNull();
  });
});
