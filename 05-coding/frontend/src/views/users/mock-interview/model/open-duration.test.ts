import { beforeEach, describe, expect, it } from "vitest";
import { recordOpenSeconds, usualOpenSeconds } from "./open-duration";

describe("open duration memory", () => {
  beforeEach(() => localStorage.clear());

  it("says nothing before the first measurement", () => {
    expect(usualOpenSeconds()).toBeNull();
  });

  it("returns the median of the last five and ignores junk", () => {
    for (const s of [30, 8, 10, 12, 9, 11]) recordOpenSeconds(s);
    recordOpenSeconds(0);
    recordOpenSeconds(Number.NaN);
    // The last five are 8, 10, 12, 9, 11.
    expect(usualOpenSeconds()).toBe(10);
  });

  it("survives corrupt storage", () => {
    localStorage.setItem("algoprep-mock-interview-open-seconds", "{oops");
    expect(usualOpenSeconds()).toBeNull();
  });
});
