import { describe, expect, it } from "vitest";
import { splitIntoParagraphs } from "./format-feedback";

describe("splitIntoParagraphs", () => {
  it("groups sentences two by two and keeps every word", () => {
    const text =
      "Câu một rõ ràng. Câu hai có dòng 5, 6, 11. Câu ba nói về độ phức tạp. Câu bốn kết luận.";
    const paragraphs = splitIntoParagraphs(text);
    expect(paragraphs).toEqual([
      "Câu một rõ ràng. Câu hai có dòng 5, 6, 11.",
      "Câu ba nói về độ phức tạp. Câu bốn kết luận.",
    ]);
    expect(paragraphs.join(" ")).toBe(text);
  });

  it("keeps a short text as one paragraph and respects the line breaks the AI wrote", () => {
    expect(splitIntoParagraphs("Một câu. Hai câu.")).toEqual([
      "Một câu. Hai câu.",
    ]);
    expect(splitIntoParagraphs("Đoạn một.\n\nĐoạn hai.")).toEqual([
      "Đoạn một.",
      "Đoạn hai.",
    ]);
  });

  it("does not cut inside a number or a code token, and returns nothing for blank text", () => {
    expect(splitIntoParagraphs("Độ phức tạp O(n) và 3.5 giây.")).toEqual([
      "Độ phức tạp O(n) và 3.5 giây.",
    ]);
    expect(splitIntoParagraphs("  \n ")).toEqual([]);
  });
});
