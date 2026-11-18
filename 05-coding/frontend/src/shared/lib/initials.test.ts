import { describe, expect, it } from "vitest";
import { initialsOf } from "./initials";

describe("initialsOf", () => {
  it("takes the first letters of the last two words, uppercase", () => {
    expect(initialsOf("Nguyễn Văn an")).toBe("VA");
  });
  it("handles one word, extra spaces and an empty name", () => {
    expect(initialsOf("  linh ")).toBe("L");
    expect(initialsOf("A   B  C")).toBe("BC");
    expect(initialsOf("   ")).toBe("");
  });
});
