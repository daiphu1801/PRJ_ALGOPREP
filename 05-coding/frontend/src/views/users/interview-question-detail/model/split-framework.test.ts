import { describe, expect, it } from "vitest";
import { splitFramework } from "./split-framework";

describe("splitFramework", () => {
  it("cuts blank-line separated sections and lifts the heading line", () => {
    expect(
      splitFramework(
        "Cách trả lời:\nÝ một.\n\nVí dụ áp dụng:\nÝ hai.\nDòng ba.",
      ),
    ).toEqual([
      { heading: "Cách trả lời", body: "Ý một." },
      { heading: "Ví dụ áp dụng", body: "Ý hai.\nDòng ba." },
    ]);
  });

  it("keeps a block with no heading whole", () => {
    expect(splitFramework("Chỉ một đoạn.")).toEqual([
      { heading: null, body: "Chỉ một đoạn." },
    ]);
  });

  it("handles Windows line endings", () => {
    expect(
      splitFramework("Cách trả lời:\r\nÝ một.\r\n\r\nĐánh đổi:\r\nÝ hai."),
    ).toEqual([
      { heading: "Cách trả lời", body: "Ý một." },
      { heading: "Đánh đổi", body: "Ý hai." },
    ]);
  });
});
