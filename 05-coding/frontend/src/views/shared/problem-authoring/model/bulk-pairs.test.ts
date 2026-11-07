import { describe, expect, it } from "vitest";
import { BULK_LIMITS, pairTestcaseFiles, type FileInfo } from "./bulk-pairs";

const f = (name: string, size = 10): FileInfo => ({ name, size });

describe("pairTestcaseFiles", () => {
  it("pairs .in with .out by base name, in natural order, with wire keys", () => {
    const { pairs, issues } = pairTestcaseFiles(
      [f("10.out"), f("2.in"), f("10.in"), f("2.out")],
      0,
    );
    expect(issues).toEqual([]);
    expect(
      pairs.map((p) => [p.key, p.base, p.input.name, p.expected.name]),
    ).toEqual([
      ["p1", "2", "2.in", "2.out"],
      ["p2", "10", "10.in", "10.out"],
    ]);
  });

  it("reports a file with another extension, and a file with none", () => {
    const { issues } = pairTestcaseFiles(
      [f("a.txt"), f("README"), f(".in")],
      0,
    );
    expect(issues).toEqual([
      { code: "extension", file: "a.txt" },
      { code: "extension", file: "README" },
      { code: "extension", file: ".in" },
    ]);
  });

  it("reports both kinds of unmatched file", () => {
    const { pairs, issues } = pairTestcaseFiles(
      [f("a.in"), f("a.out"), f("b.in"), f("c.out")],
      0,
    );
    expect(pairs).toHaveLength(1);
    expect(issues).toEqual([
      { code: "unmatched", file: "b.in" },
      { code: "unmatched", file: "c.out" },
    ]);
  });

  it("reports a duplicate base name (extension case does not matter) and keeps the first", () => {
    const { pairs, issues } = pairTestcaseFiles(
      [f("a.in"), f("a.IN"), f("a.out")],
      0,
    );
    expect(issues).toEqual([{ code: "duplicate", file: "a.IN" }]);
    expect(pairs[0]!.input.name).toBe("a.in");
  });

  it("reports empty and oversized sides", () => {
    const { issues } = pairTestcaseFiles(
      [
        f("a.in", 0),
        f("a.out"),
        f("b.in", BULK_LIMITS.maxSideBytes + 1),
        f("b.out"),
      ],
      0,
    );
    expect(issues).toEqual(
      expect.arrayContaining([
        { code: "empty", file: "a.in" },
        { code: "sideTooLarge", file: "b.in" },
      ]),
    );
  });

  it("accepts exactly 100 pairs and reports 101", () => {
    const make = (n: number) =>
      Array.from({ length: n }, (_, i) => [f(`${i}.in`), f(`${i}.out`)]).flat();
    expect(pairTestcaseFiles(make(100), 0).issues).toEqual([]);
    expect(pairTestcaseFiles(make(101), 0).issues).toEqual([
      { code: "tooManyPairs", count: 101, limit: 100 },
    ]);
  });

  it("reports a total above 256 MB and a testcase count above 500 with the existing ones", () => {
    const big = BULK_LIMITS.maxSideBytes;
    const total = pairTestcaseFiles(
      [
        f("a.in", big),
        f("a.out", big),
        f("b.in", big),
        f("b.out", big),
        f("c.in", 1),
        f("c.out", 1),
      ],
      0,
    );
    expect(total.issues).toEqual([
      { code: "totalTooLarge", limit: BULK_LIMITS.maxTotalBytes },
    ]);
    expect(pairTestcaseFiles([f("a.in"), f("a.out")], 499).issues).toEqual([]);
    expect(
      pairTestcaseFiles([f("a.in"), f("a.out"), f("b.in"), f("b.out")], 499)
        .issues,
    ).toEqual([{ code: "tooManyTestcases", count: 501, limit: 500 }]);
  });
});
