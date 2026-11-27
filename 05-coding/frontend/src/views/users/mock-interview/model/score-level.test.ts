import { describe, expect, it } from "vitest";
import { scoreLevel } from "./score-level";

describe("scoreLevel", () => {
  it("cuts at 5 and 8", () => {
    expect(scoreLevel(4.99)).toBe("weak");
    expect(scoreLevel(5)).toBe("fair");
    expect(scoreLevel(7.9)).toBe("fair");
    expect(scoreLevel(8)).toBe("good");
    expect(scoreLevel(0)).toBe("weak");
  });
});
