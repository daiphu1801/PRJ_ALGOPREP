import { describe, expect, it } from "vitest";
import { sanitizeHtml } from "./sanitize-html";

describe("sanitizeHtml", () => {
  // This test deliberately locks in "fail loud": once someone implements a real sanitizer, it
  // turns red and forces them to rewrite the test with real XSS vectors, instead of letting a
  // stub function slip through unnoticed.
  it("throws because no real sanitizer has been chosen yet", () => {
    expect(() => sanitizeHtml("<p>bất kỳ</p>")).toThrow(/chưa được triển khai/);
  });
});
