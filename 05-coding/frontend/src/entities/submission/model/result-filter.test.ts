import { describe, expect, it } from "vitest";
import { matchesResultFilter } from "./result-filter";

// DEC-2026-1014-instructor-submissions-list-details: "Chưa đạt" is judged and failed only; a submission
// still judging, or ended by a system error (not the learner's fault), shows only under "Tất cả".
describe("instructor submission result filter", () => {
  it("accepted matches only ACCEPTED", () => {
    expect(matchesResultFilter("ACCEPTED", "accepted")).toBe(true);
    expect(matchesResultFilter("WRONG_ANSWER", "accepted")).toBe(false);
  });

  it("failed matches judged-and-failed submissions only", () => {
    for (const status of [
      "WRONG_ANSWER",
      "TIME_LIMIT_EXCEEDED",
      "RUNTIME_ERROR",
      "COMPILE_ERROR",
    ] as const) {
      expect(matchesResultFilter(status, "failed")).toBe(true);
    }
    for (const status of [
      "PENDING",
      "COMPILING",
      "JUDGING",
      "SYSTEM_ERROR",
      "ACCEPTED",
    ] as const) {
      expect(matchesResultFilter(status, "failed")).toBe(false);
    }
  });

  it("all matches every status", () => {
    expect(matchesResultFilter("SYSTEM_ERROR", "all")).toBe(true);
    expect(matchesResultFilter("JUDGING", "all")).toBe(true);
  });
});
