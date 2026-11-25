import { describe, expect, it } from "vitest";
import { parseTsv } from "./parse-tsv";

describe("parseTsv", () => {
  it("keeps newlines, tabs and doubled quotes of a quoted cell", () => {
    expect(
      parseTsv('a\t"x = 1\n\tif x:\n\tprint(""hi"")"\tc\r\nd\te\tf\r\n'),
    ).toEqual([
      ["a", 'x = 1\n\tif x:\n\tprint("hi")', "c"],
      ["d", "e", "f"],
    ]);
  });

  it("keeps a quote that is not at the cell start", () => {
    expect(parseTsv('print("a")\t5" screen')).toEqual([
      ['print("a")', '5" screen'],
    ]);
  });

  it("keeps leading zeros and empty cells, drops blank rows and the BOM", () => {
    expect(parseTsv("\uFEFF007\t\t1/2\n\t\t\n\nb\t")).toEqual([
      ["007", "", "1/2"],
      ["b", ""],
    ]);
  });

  it("returns nothing for blank text", () => {
    expect(parseTsv(" \n\t\n")).toEqual([]);
  });
});
