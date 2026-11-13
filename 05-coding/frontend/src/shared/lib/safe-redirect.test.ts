import { describe, expect, it } from "vitest";
import { isSafeRedirect } from "./safe-redirect";

describe("isSafeRedirect", () => {
  it("accepts paths inside the app, with query and hash", () => {
    expect(isSafeRedirect("/dashboard")).toBe(true);
    expect(isSafeRedirect("/admin/users?page=2#top")).toBe(true);
  });

  it("rejects other sites and anything that is not a path", () => {
    expect(isSafeRedirect("//evil.example")).toBe(false);
    expect(isSafeRedirect("/\\evil.example")).toBe(false);
    expect(isSafeRedirect("https://evil.example")).toBe(false);
    expect(isSafeRedirect("javascript:alert(1)")).toBe(false);
    expect(isSafeRedirect("dashboard")).toBe(false);
    expect(isSafeRedirect("")).toBe(false);
    expect(isSafeRedirect(null)).toBe(false);
    expect(isSafeRedirect(undefined)).toBe(false);
  });
});
