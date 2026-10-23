import { describe, expect, it } from "vitest";
import { lineDiff } from "./line-diff";

describe("lineDiff", () => {
  it("marks unchanged, removed and added lines", () => {
    expect(lineDiff(["a", "b", "c"], ["a", "x", "c"])).toEqual([
      { kind: "same", text: "a" },
      { kind: "del", text: "b" },
      { kind: "add", text: "x" },
      { kind: "same", text: "c" },
    ]);
  });

  it("returns only same lines for identical input", () => {
    expect(lineDiff(["a"], ["a"])).toEqual([{ kind: "same", text: "a" }]);
  });
});
