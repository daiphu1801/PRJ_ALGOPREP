import { describe, expect, it } from "vitest";
import { isGuestReadablePath } from "./guest-paths";

describe("isGuestReadablePath", () => {
  it("opens the problem list and a single problem overview", () => {
    expect(isGuestReadablePath("/problems")).toBe(true);
    expect(isGuestReadablePath("/problems/")).toBe(true);
    expect(isGuestReadablePath("/problems/abc-123")).toBe(true);
  });

  it("keeps everything else behind a session", () => {
    for (const path of [
      "/",
      "/dashboard",
      "/workspace",
      "/workspace/abc",
      "/problems/abc/extra",
      "/problems-old",
      "/saved",
      "/classes",
      "/instructor/problems",
      "/admin/problems",
    ]) {
      expect(isGuestReadablePath(path), path).toBe(false);
    }
  });
});
