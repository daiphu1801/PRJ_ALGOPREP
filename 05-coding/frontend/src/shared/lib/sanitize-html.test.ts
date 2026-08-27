import { describe, expect, it } from "vitest";
import { sanitizeHtml } from "./sanitize-html";

describe("sanitizeHtml", () => {
  // Test này khoá chủ đích "fail loud": khi ai đó triển khai sanitizer thật, test sẽ đỏ và
  // buộc người đó viết lại test bằng các vector XSS thật, thay vì để một hàm giả đi qua.
  it("ném lỗi vì sanitizer thật chưa được chọn", () => {
    expect(() => sanitizeHtml("<p>bất kỳ</p>")).toThrow(/chưa được triển khai/);
  });
});
