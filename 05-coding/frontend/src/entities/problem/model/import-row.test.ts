import { describe, expect, it } from "vitest";
import type { ProblemDraft } from "./draft-types";
import {
  IMPORT_HEADER,
  blankImportRow,
  checkImportRow,
  gridToRows,
  repeatedTitles,
  toImportDraft,
  type ImportCatalogue,
  type ImportRow,
} from "./import-row";

const catalogue: ImportCatalogue = {
  levels: [
    { key: "EASY", label: "Dễ" },
    { key: "HARD", label: "Khó" },
  ],
  topics: [
    { key: "t1", label: "Mảng" },
    { key: "t2", label: "Đồ thị" },
  ],
  languages: ["Java", "C++", "Python"],
};

const ids = () => {
  let n = 0;
  return () => `r${(n += 1)}`;
};

const full = (over: Partial<ImportRow> = {}): ImportRow => ({
  ...blankImportRow("r1"),
  title: "Tổng",
  level: "de",
  topics: "mang, do thi",
  statement: "Đề",
  stdin: "n",
  stdout: "tổng",
  solution: "print(1)",
  cases: Array.from({ length: 8 }, (_, i) => ({
    input: `${i}`,
    output: `o${i}`,
    note: i < 2 ? `g${i}` : "",
  })),
  ...over,
});

const check = (over: Partial<ImportRow>, repeated = false) =>
  checkImportRow(full(over), catalogue, repeated);
const codes = (over: Partial<ImportRow>) =>
  check(over).issues.map((issue) => issue.code);

describe("gridToRows", () => {
  it("matches columns in any order, case and accents, Vietnamese or English", () => {
    const { rows, hasHeader, unknownColumns } = gridToRows(
      [
        ["OUTPUT 1", "tieu de", "Input1", "Ghi chú riêng", "Difficulty"],
        ["5", "Tổng\r\nhai", "1 2", "bỏ", "Dễ"],
      ],
      20,
      ids(),
    );
    expect(hasHeader).toBe(true);
    expect(unknownColumns).toEqual(["Ghi chú riêng"]);
    expect(rows[0]).toMatchObject({
      id: "r1",
      title: "Tổng\nhai",
      level: "Dễ",
      cases: [{ input: "1 2", output: "5", note: "" }],
    });
  });

  it("reads the template order when there is no header row", () => {
    const cells = IMPORT_HEADER.map((name) => `<${name}>`);
    const { rows, hasHeader } = gridToRows([cells], 20, ids());
    expect(hasHeader).toBe(false);
    expect(rows[0]!.title).toBe("<Tiêu đề>");
    expect(rows[0]!.cases).toHaveLength(8);
    expect(rows[0]!.cases[1]).toEqual({
      input: "<Input 2>",
      output: "<Output 2>",
      note: "<Giải thích 2>",
    });
  });

  it("takes up to the room and counts the rest", () => {
    const grid = [["Tiêu đề", "Độ khó"], ["a"], ["b"], ["c"]];
    const result = gridToRows(grid, 2, ids());
    expect(result.rows.map((row) => row.title)).toEqual(["a", "b"]);
    expect(result.leftOver).toBe(1);
    expect(gridToRows(grid, 0, ids())).toMatchObject({ rows: [], leftOver: 3 });
  });

  it("drops empty testcases, keeps half ones and stops at Input 20", () => {
    const { rows, unknownColumns } = gridToRows(
      [
        ["Tiêu đề", "Input 1", "Output 1", "Input 3", "Input 20", "Input 21"],
        ["a", "", "", "x", "y", "z"],
      ],
      20,
      ids(),
    );
    expect(rows[0]!.cases.map((entry) => entry.input)).toEqual(["x", "y"]);
    expect(unknownColumns).toEqual(["Input 21"]);
  });
});

describe("checkImportRow", () => {
  it("accepts a complete row", () => {
    expect(check({})).toEqual({ issues: [], gaps: [], caseCount: 8 });
  });

  it.each<[Partial<ImportRow>, string]>([
    [{ title: " " }, "titleMissing"],
    [{ title: "a".repeat(256) }, "titleTooLong"],
    [{ level: "" }, "levelMissing"],
    [{ level: "Rất khó" }, "levelUnknown"],
    [{ topics: "Mảng, Lạ" }, "topicUnknown"],
    [{ statement: "\n" }, "statementMissing"],
    [{ statement: "a".repeat(100_001) }, "statementTooLong"],
    [{ constraints: "a".repeat(20_001) }, "constraintsTooLong"],
    [{ language: "Ruby" }, "languageUnknown"],
    [{ solution: "đ".repeat(33_000) }, "solutionTooLong"],
    [{ cases: [{ input: "1", output: "", note: "" }] }, "caseHalf"],
    [
      { cases: [{ input: "đ".repeat(600_000), output: "1", note: "" }] },
      "caseTooLarge",
    ],
  ])("blocks %#", (over, code) => {
    expect(codes(over)).toEqual([code]);
  });

  it("names the unknown topic and the half testcase", () => {
    expect(check({ topics: "Lạ" }).issues).toEqual([
      { code: "topicUnknown", value: "Lạ" },
    ]);
    expect(
      check({
        cases: [
          { input: "1", output: "1", note: "" },
          { input: "", output: "2", note: "" },
        ],
      }).issues,
    ).toEqual([{ code: "caseHalf", value: "2" }]);
  });

  it("only lists what is missing when the row can be a draft", () => {
    expect(
      check({ stdin: "", stdout: " ", solution: "", language: "?", cases: [] }),
    ).toEqual({
      issues: [],
      gaps: ["stdin", "stdout", "solution", "cases"],
      caseCount: 0,
    });
  });

  it("flags a title repeated in the table", () => {
    const rows = [
      full(),
      full({ id: "r2", title: "TONG " }),
      full({ title: "Khác" }),
    ];
    expect([...repeatedTitles(rows)]).toEqual(["tong"]);
    expect(check({}, true).issues).toEqual([{ code: "titleRepeated" }]);
  });
});

describe("toImportDraft", () => {
  const base = {
    title: "cũ",
    tags: ["giữ"],
    limits: { timeLimitMs: 1000 },
    difficulty: "HARD",
    solutionLanguage: "Java",
    examples: [{ id: "ex-1" }],
    spec: { signature: { functionName: "" }, matchingStrategy: "EXACT" },
    status: "draft",
  } as unknown as ProblemDraft;

  it("maps names to keys, makes the first two cases public examples and keeps the rest of the base", () => {
    const draft = toImportDraft(full({ language: "py" }), base, catalogue);
    expect(draft).toMatchObject({
      title: "Tổng",
      topic: "t1",
      otherTopicIds: ["t2"],
      difficulty: "EASY",
      solutionLanguage: "Python",
      tags: ["giữ"],
      limits: { timeLimitMs: 1000 },
      spec: {
        signature: { functionName: "" },
        matchingStrategy: "EXACT",
        stdinFormat: "n",
        stdoutFormat: "tổng",
      },
    });
    expect(draft.examples).toEqual([
      { id: "ex-1", input: "0", output: "o0", explanation: "g0" },
      { id: "import-2", input: "1", output: "o1", explanation: "g1" },
    ]);
    expect(draft.testcases.map((row) => row.visibility)).toEqual([
      "public",
      "public",
      ...Array.from({ length: 6 }, () => "hidden"),
    ]);
  });
});
