import { describe, expect, it } from "vitest";
import { cn } from "./cn";

describe("cn", () => {
  it("merges classNames and dedupes conflicting Tailwind classes, last one wins", () => {
    expect(cn("px-2 py-1", "px-4")).toBe("py-1 px-4");
  });

  it("skips falsy values", () => {
    expect(cn("a", false, undefined, "b")).toBe("a b");
  });
});
