import { describe, expect, it } from "vitest";
import { instructorActiveHrefOverride } from "./instructor-sidebar";

describe("instructor sidebar: item kept lit on the submission result page", () => {
  it("follows the calling screen named by from=", () => {
    const path = "/instructor/submissions/SB-90412";
    expect(instructorActiveHrefOverride(path, "student")).toBe(
      "/instructor/students",
    );
    expect(instructorActiveHrefOverride(path, "class")).toBe(
      "/instructor/classes",
    );
    expect(instructorActiveHrefOverride(path, "submissions")).toBe(
      "/instructor/submissions",
    );
    expect(instructorActiveHrefOverride(path, null)).toBe(
      "/instructor/submissions",
    );
  });

  it("keeps the calling item lit on the student detail (from=students or from=submissions)", () => {
    const path = "/instructor/classes/c1/students/s1";
    expect(instructorActiveHrefOverride(path, "students")).toBe(
      "/instructor/students",
    );
    expect(instructorActiveHrefOverride(path, "submissions")).toBe(
      "/instructor/submissions",
    );
    // No hint, or an unknown one: "Lớp của tôi" through the prefix match.
    expect(instructorActiveHrefOverride(path, null)).toBeUndefined();
    expect(instructorActiveHrefOverride(path, "x")).toBeUndefined();
  });

  it("leaves every other route to the pathname match", () => {
    expect(
      instructorActiveHrefOverride("/instructor/submissions", "student"),
    ).toBeUndefined();
    expect(
      instructorActiveHrefOverride("/instructor/classes/c1", "student"),
    ).toBeUndefined();
  });
});
