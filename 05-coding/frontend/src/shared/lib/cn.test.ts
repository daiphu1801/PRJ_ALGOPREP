import { describe, expect, it } from "vitest";
import { cn } from "./cn";

describe("cn", () => {
  it("gộp className và loại bỏ trùng lặp Tailwind theo thứ tự sau thắng", () => {
    expect(cn("px-2 py-1", "px-4")).toBe("py-1 px-4");
  });

  it("bỏ qua giá trị falsy", () => {
    expect(cn("a", false, undefined, "b")).toBe("a b");
  });
});
