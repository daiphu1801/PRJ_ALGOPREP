import { describe, expect, it } from "vitest";
import { csvCell, parseCsv } from "./parse-csv";

describe("parseCsv", () => {
  it("splits records and cells, drops blank lines and the BOM", () => {
    expect(parseCsv("\uFEFFa,b\r\n\r\nc,d\n")).toEqual([
      { line: 1, cells: ["a", "b"] },
      { line: 3, cells: ["c", "d"] },
    ]);
  });

  it("keeps commas, escaped quotes and newlines inside quoted cells and tracks the start line", () => {
    const records = parseCsv('x,"a, ""b""\nsecond"\ny,z');
    expect(records[0]).toEqual({ line: 1, cells: ["x", 'a, "b"\nsecond'] });
    expect(records[1]).toEqual({ line: 3, cells: ["y", "z"] });
  });

  it("round-trips through csvCell", () => {
    const value = 'he said "hi", then\nleft';
    expect(parseCsv(`k,${csvCell(value)}`)[0]?.cells[1]).toBe(value);
  });
});
