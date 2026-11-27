// Rules of the bulk problem import (F2-19, SHR0204): how a grid of cells becomes problems, what stops a row
// from being created, and what keeps a created one from being published. No React and no i18n: the screen
// turns the codes into text. The limits are the server's (03-dd/api/F2_problem-bank.md sections 36 and 42);
// the server still enforces them, this only reports early.
import type { ProblemDraft, Testcase } from "./draft-types";

/** Rows per run: the cap of a bulk publish (#28). */
export const IMPORT_MAX_ROWS = 20;
/** F2-15: approved testcases a problem needs to be published. */
export const IMPORT_MIN_CASES = 8;
export const IMPORT_MAX_CASES = 20;
/** The first cases are Sample testcases and worked examples at the same time (F2-15 wants two of each). */
export const IMPORT_SAMPLES = 2;

const MAX = {
  title: 255,
  statement: 100_000,
  constraints: 20_000,
  solutionBytes: 64 * 1024,
  sideBytes: 1024 * 1024,
};

export type ImportCase = { input: string; output: string; note: string };

/** One problem as typed. Difficulty, topics and language stay text until they are matched to the catalogue. */
export type ImportRow = {
  id: string;
  title: string;
  level: string;
  /** Topic names separated by commas. */
  topics: string;
  statement: string;
  stdin: string;
  stdout: string;
  constraints: string;
  solution: string;
  language: string;
  cases: ImportCase[];
};

type TextField = Exclude<keyof ImportRow, "id" | "cases">;

export type ImportCatalogue = {
  levels: readonly { key: string; label: string }[];
  topics: readonly { key: string; label: string }[];
  /** Names the judge accepts now, as the author writes them: "Java", "C++", "Python". */
  languages: readonly string[];
};

export type ImportIssueCode =
  | "titleMissing"
  | "titleTooLong"
  | "titleRepeated"
  | "levelMissing"
  | "levelUnknown"
  | "topicUnknown"
  | "statementMissing"
  | "statementTooLong"
  | "constraintsTooLong"
  | "languageUnknown"
  | "solutionTooLong"
  | "caseHalf"
  | "caseTooLarge";

/** `value` is what the message shows: the unknown name, or the testcase number. */
export type ImportIssue = { code: ImportIssueCode; value?: string };

export type ImportGap = "stdin" | "stdout" | "solution" | "cases";

export type ImportCheck = {
  /** Non-empty: the row cannot be created. */
  issues: ImportIssue[];
  /** Non-empty: it can only be kept as a draft. */
  gaps: ImportGap[];
  /** Cases with both sides. */
  caseCount: number;
};

/** Case, accents and repeated spaces do not matter: "de" matches "Dễ". */
export const foldText = (text: string) =>
  text
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/gi, "d")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();

const COLUMNS: [TextField, string[]][] = [
  ["title", ["Tiêu đề", "Title"]],
  ["level", ["Độ khó", "Difficulty"]],
  ["topics", ["Chủ đề", "Topics"]],
  ["statement", ["Đề bài", "Statement"]],
  ["stdin", ["Định dạng vào", "Input format"]],
  ["stdout", ["Định dạng ra", "Output format"]],
  ["constraints", ["Ràng buộc", "Constraints"]],
  ["solution", ["Lời giải", "Solution"]],
  ["language", ["Ngôn ngữ", "Language"]],
];

/** Column order of the template, used for cells pasted without their header row. */
export const IMPORT_HEADER: string[] = [
  ...COLUMNS.map(([, names]) => names[0]!),
  ...Array.from({ length: IMPORT_MIN_CASES }, (_, i) => [
    `Input ${i + 1}`,
    `Output ${i + 1}`,
    ...(i < IMPORT_SAMPLES ? [`Giải thích ${i + 1}`] : []),
  ]).flat(),
];

type Target = { field: TextField } | { index: number; side: keyof ImportCase };

const FIELD_BY_NAME = new Map(
  COLUMNS.flatMap(([field, names]) =>
    names.map((name) => [foldText(name), field] as const),
  ),
);
const CASE_COLUMN = /^(input|output|giai thich|explanation) ?(\d{1,2})$/;

function targetOf(header: string): Target | null {
  const name = foldText(header);
  const field = FIELD_BY_NAME.get(name);
  if (field) return { field };
  const match = CASE_COLUMN.exec(name);
  if (!match) return null;
  const index = Number(match[2]) - 1;
  if (index < 0 || index >= IMPORT_MAX_CASES) return null;
  return {
    index,
    side:
      match[1] === "input"
        ? "input"
        : match[1] === "output"
          ? "output"
          : "note",
  };
}

export function blankImportRow(id: string): ImportRow {
  return {
    id,
    title: "",
    level: "",
    topics: "",
    statement: "",
    stdin: "",
    stdout: "",
    constraints: "",
    solution: "",
    language: "Python",
    cases: [],
  };
}

export type GridResult = {
  rows: ImportRow[];
  hasHeader: boolean;
  /** Header cells that match no known column. */
  unknownColumns: string[];
  /** Rows that did not fit under the cap. */
  leftOver: number;
};

/**
 * A grid of cells to rows. The first row is the header when at least two of its cells are known columns;
 * without one the cells are read in template order.
 */
export function gridToRows(
  grid: readonly (readonly string[])[],
  room: number,
  nextId: () => string,
): GridResult {
  const hasHeader =
    (grid[0] ?? []).filter((cell) => targetOf(cell)).length >= 2;
  const header = hasHeader ? grid[0]! : IMPORT_HEADER;
  const body = hasHeader ? grid.slice(1) : grid;
  const targets = header.map(targetOf);

  const rows = body.slice(0, Math.max(0, room)).map((cells) => {
    const row = blankImportRow(nextId());
    const cases = new Map<number, ImportCase>();
    cells.forEach((cell, column) => {
      const target = targets[column];
      const value = cell.replace(/\r\n?/g, "\n");
      if (!target || value === "") return;
      if ("field" in target) {
        row[target.field] = value;
        return;
      }
      const entry = cases.get(target.index) ?? {
        input: "",
        output: "",
        note: "",
      };
      entry[target.side] = value;
      cases.set(target.index, entry);
    });
    row.cases = [...cases.entries()]
      .sort(([left], [right]) => left - right)
      .map(([, entry]) => entry)
      .filter((entry) => entry.input !== "" || entry.output !== "");
    return row;
  });
  return {
    rows,
    hasHeader,
    unknownColumns: header.filter(
      (cell, column) => cell.trim() !== "" && !targets[column],
    ),
    leftOver: body.length - rows.length,
  };
}

const byteLength = (text: string) => new TextEncoder().encode(text).length;

const LANGUAGE_ALIAS: Record<string, string> = {
  py: "Python",
  python3: "Python",
  cpp: "C++",
};

export const resolveLanguage = (
  text: string,
  languages: readonly string[],
): string | undefined => {
  const wanted = LANGUAGE_ALIAS[foldText(text)] ?? text;
  return languages.find((name) => foldText(name) === foldText(wanted));
};

export const resolveLevel = (text: string, levels: ImportCatalogue["levels"]) =>
  levels.find((level) => foldText(level.label) === foldText(text));

const topicNames = (text: string) =>
  text
    .split(",")
    .map((name) => name.trim())
    .filter((name) => name !== "");

/** The row's title is `repeated` when another row of the table has the same one. */
export function checkImportRow(
  row: ImportRow,
  catalogue: ImportCatalogue,
  repeated: boolean,
): ImportCheck {
  const issues: ImportIssue[] = [];
  const add = (code: ImportIssueCode, value?: string) =>
    issues.push(value === undefined ? { code } : { code, value });

  const title = row.title.trim();
  if (!title) add("titleMissing");
  else if (title.length > MAX.title) add("titleTooLong");
  else if (repeated) add("titleRepeated");

  if (!row.level.trim()) add("levelMissing");
  else if (!resolveLevel(row.level, catalogue.levels))
    add("levelUnknown", row.level.trim());

  for (const name of topicNames(row.topics))
    if (!catalogue.topics.some((t) => foldText(t.label) === foldText(name)))
      add("topicUnknown", name);

  if (!row.statement.trim()) add("statementMissing");
  else if (row.statement.length > MAX.statement) add("statementTooLong");
  if (row.constraints.length > MAX.constraints) add("constraintsTooLong");

  if (row.solution.trim()) {
    if (!resolveLanguage(row.language, catalogue.languages))
      add("languageUnknown", row.language.trim());
    if (byteLength(row.solution) > MAX.solutionBytes) add("solutionTooLong");
  }

  let caseCount = 0;
  row.cases.forEach((entry, index) => {
    const number = String(index + 1);
    if (!entry.input && !entry.output) return;
    if (!entry.input || !entry.output) add("caseHalf", number);
    else caseCount += 1;
    if (
      byteLength(entry.input) > MAX.sideBytes ||
      byteLength(entry.output) > MAX.sideBytes
    )
      add("caseTooLarge", number);
  });

  const gaps: ImportGap[] = [];
  if (!row.stdin.trim()) gaps.push("stdin");
  if (!row.stdout.trim()) gaps.push("stdout");
  if (!row.solution.trim()) gaps.push("solution");
  if (caseCount < IMPORT_MIN_CASES) gaps.push("cases");
  return { issues, gaps, caseCount };
}

/** Titles used by more than one row, folded. */
export function repeatedTitles(rows: readonly ImportRow[]): Set<string> {
  const seen = new Set<string>();
  const repeated = new Set<string>();
  for (const row of rows) {
    const title = foldText(row.title);
    if (!title) continue;
    if (seen.has(title)) repeated.add(title);
    seen.add(title);
  }
  return repeated;
}

/**
 * The row written over `base`: a blank draft for a new problem (it carries the system's default limits), or
 * what the server holds for one created earlier, so fields this table does not show are kept. Standard I/O
 * only, so the spec never gets a signature.
 */
export function toImportDraft(
  row: ImportRow,
  base: ProblemDraft,
  catalogue: ImportCatalogue,
): ProblemDraft {
  const topics = topicNames(row.topics).flatMap((name) => {
    const topic = catalogue.topics.find(
      (item) => foldText(item.label) === foldText(name),
    );
    return topic ? [topic.key] : [];
  });
  const cases = row.cases.filter((entry) => entry.input && entry.output);
  return {
    ...base,
    title: row.title.trim(),
    body: row.statement,
    constraints: row.constraints,
    topic: topics[0] ?? "",
    otherTopicIds: [...new Set(topics.slice(1))],
    difficulty:
      resolveLevel(row.level, catalogue.levels)?.key ?? base.difficulty,
    solution: row.solution,
    solutionLanguage:
      resolveLanguage(row.language, catalogue.languages) ??
      base.solutionLanguage,
    examples: cases.slice(0, IMPORT_SAMPLES).map((entry, index) => ({
      // The stored id is kept, so an unchanged example is not rewritten.
      id: base.examples[index]?.id ?? `import-${index + 1}`,
      input: entry.input,
      output: entry.output,
      explanation: entry.note,
    })),
    spec: { ...base.spec, stdinFormat: row.stdin, stdoutFormat: row.stdout },
    testcases: cases.map((entry, index): Testcase => ({
      id: `import-${index + 1}`,
      input: entry.input,
      expected: entry.output,
      visibility: index < IMPORT_SAMPLES ? "public" : "hidden",
      origin: "manual",
      approved: true,
    })),
  };
}
