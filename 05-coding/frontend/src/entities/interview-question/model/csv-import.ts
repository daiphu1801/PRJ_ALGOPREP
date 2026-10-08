// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// CSV import for the interview question bank (SHR0301, owner decision 2026-10-08). The column set is
// fixed by the owner on 2026-10-08 (BD SHR0301 Q11): required columns topic, level, question, rubric
// (a rubric cell may be empty, the column may not); optional columns follow_ups, content,
// suggested_approach, core_keywords, sample_answer. A question already in the bank (same text, case
// insensitive) is skipped, never overwritten. Validation is per row: a bad row is reported and
// skipped, good rows still import.
import { parseCsv, csvCell } from "@/shared/lib";
import type { InterviewQuestion } from "./types";

export const CSV_COLUMNS = [
  "topic",
  "level",
  "question",
  "rubric",
  "follow_ups",
  "content",
  "suggested_approach",
  "core_keywords",
  "sample_answer",
] as const;
const REQUIRED = ["topic", "level", "question"] as const;

/** Lists are packed into one cell: items split by "|", rubric items as "label:weight". */
export const CSV_TEMPLATE = [
  CSV_COLUMNS.join(","),
  [
    "Lý thuyết CS",
    "Trung bình",
    "Hash table xử lý collision bằng cách nào?",
    "Định nghĩa collision:30|So sánh hai cơ chế:40|Chốt bằng load factor:30",
    "Ngưỡng load factor nào thì cần resize?|Vì sao open addressing thắng khi load factor thấp?",
    "Giải thích cách hash table xử lý hai khoá trùng chỉ số.",
    "Chaining|Open addressing",
    "collision|load factor",
    "",
  ]
    .map(csvCell)
    .join(","),
].join("\r\n");

export const CSV_MAX_ROWS = 500;

export type QuestionDraft = Pick<
  InterviewQuestion,
  "topic" | "level" | "question" | "followUps" | "rubric"
> &
  Partial<
    Pick<
      InterviewQuestion,
      "content" | "suggestedApproach" | "coreKeywords" | "sampleAnswerFramework"
    >
  >;

export type CsvRowErrorCode =
  | "missingField"
  | "unknownTopic"
  | "unknownLevel"
  | "badRubric"
  | "rubricSum"
  | "duplicate";

export type CsvRowError = {
  line: number;
  code: CsvRowErrorCode;
  detail?: string;
};

export type CsvFileError = "empty" | "missingColumns" | "tooManyRows";

export type CsvImportParse = {
  drafts: QuestionDraft[];
  errors: CsvRowError[];
  /** Data rows read, valid or not. */
  total: number;
  /** Set when the file as a whole cannot be read; `detail` lists the missing column names. */
  fileError?: { code: CsvFileError; detail?: string };
};

type Option = { key: string; label: string };

const norm = (value: string) => value.trim().toLocaleLowerCase("vi");

// Accepts the stable key ("MEDIUM") or the label the admin sees ("Trung bình"), either case.
const resolve = (options: readonly Option[], raw: string) =>
  options.find(
    (option) =>
      norm(option.key) === norm(raw) || norm(option.label) === norm(raw),
  )?.key;

function parseRubric(raw: string): QuestionDraft["rubric"] | null {
  if (!raw.trim()) return [];
  const rubric: QuestionDraft["rubric"] = [];
  for (const part of raw.split("|")) {
    const at = part.lastIndexOf(":");
    const label = part.slice(0, at).trim();
    const weight = Number(part.slice(at + 1));
    if (
      at < 0 ||
      !label ||
      !Number.isInteger(weight) ||
      weight < 0 ||
      weight > 100
    )
      return null;
    rubric.push({ label, weight });
  }
  return rubric;
}

export function parseQuestionsCsv(
  text: string,
  ctx: {
    topics: readonly Option[];
    levels: readonly Option[];
    /** Question texts already in the bank; a row repeating one is skipped. */
    existing: readonly string[];
  },
): CsvImportParse {
  const records = parseCsv(text);
  const empty: CsvImportParse = { drafts: [], errors: [], total: 0 };
  const header = records[0];
  if (!header) return { ...empty, fileError: { code: "empty" } };

  const index = new Map(header.cells.map((name, at) => [norm(name), at]));
  const missing = [...REQUIRED, "rubric"].filter((name) => !index.has(name));
  if (missing.length > 0)
    return {
      ...empty,
      fileError: { code: "missingColumns", detail: missing.join(", ") },
    };

  const rows = records.slice(1);
  if (rows.length === 0) return { ...empty, fileError: { code: "empty" } };
  if (rows.length > CSV_MAX_ROWS)
    return {
      ...empty,
      fileError: { code: "tooManyRows", detail: String(CSV_MAX_ROWS) },
    };

  const seen = new Set(ctx.existing.map(norm));
  const drafts: QuestionDraft[] = [];
  const errors: CsvRowError[] = [];

  for (const { line, cells } of rows) {
    const cell = (name: string) => (cells[index.get(name) ?? -1] ?? "").trim();
    const fail = (code: CsvRowErrorCode, detail?: string) =>
      errors.push({ line, code, detail });

    const question = cell("question");
    const missingField = REQUIRED.find((name) => !cell(name));
    if (missingField) {
      fail("missingField", missingField);
      continue;
    }
    const topic = resolve(ctx.topics, cell("topic"));
    if (!topic) {
      fail("unknownTopic", cell("topic"));
      continue;
    }
    const level = resolve(ctx.levels, cell("level"));
    if (!level) {
      fail("unknownLevel", cell("level"));
      continue;
    }
    const rubric = parseRubric(cell("rubric"));
    if (!rubric) {
      fail("badRubric");
      continue;
    }
    // Same rule as the authoring form: a rubric with criteria must total exactly 100.
    if (
      rubric.length > 0 &&
      rubric.reduce((sum, item) => sum + item.weight, 0) !== 100
    ) {
      fail("rubricSum");
      continue;
    }
    if (seen.has(norm(question))) {
      fail("duplicate");
      continue;
    }
    seen.add(norm(question));
    const list = (name: string) =>
      cell(name)
        .split("|")
        .map((item) => item.trim())
        .filter(Boolean);
    drafts.push({
      topic,
      level,
      question,
      followUps: list("follow_ups"),
      rubric,
      content: cell("content") || undefined,
      suggestedApproach: list("suggested_approach"),
      coreKeywords: list("core_keywords"),
      sampleAnswerFramework: cell("sample_answer"),
    });
  }
  return { drafts, errors, total: rows.length };
}
