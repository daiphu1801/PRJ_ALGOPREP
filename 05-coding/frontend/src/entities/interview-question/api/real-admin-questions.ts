// Real calls behind SHR0301 / SHR0302 / SHR0303 (03-dd/api/F6_interview-bank.md #19 to #24, DD sections
// 19 to 24). The record is the wire: AdminQuestionsController and AdminQuestionBulkController.
// Paths are relative to /api/v1. Every error is rethrown as `ApiError` (INB-*, shown through
// `useApiErrorMessage`); a failed call is never turned into an outcome object.
import { api } from "@/shared/api";
import type { PageMeta } from "@/shared/api";

const BASE = "/interview-bank/admin/questions";

export type AdminQuestionRef = { code: string; displayName: string };

export type AdminQuestionRow = {
  /** UUID in real mode (route key, request key); the question code in mock mode. */
  id: string;
  /** Shown as `IQ-%03d`; a label only, never a key (BD SHR0301 Q5). */
  displayNo: number;
  level: AdminQuestionRef;
  topic: AdminQuestionRef;
  title: string;
  followUpCount: number;
  rubricCount: number;
  usageCount: number;
  /** 0-100; null until the first COMPLETED practice attempt (shown as "-"). */
  averageScorePercent: number | null;
};

export type AdminQuestionList = {
  items: AdminQuestionRow[];
  page: PageMeta;
  /** `meta.total`: every ACTIVE question of the bank, filter ignored. */
  total: number;
};

export type AdminRubricItem = {
  criterionCode: string;
  weightPercent: number;
  description: string | null;
};

export type AdminQuestionDetail = {
  id: string;
  displayNo: number;
  topicId: string;
  levelId: string;
  title: string;
  contentMarkdown: string | null;
  followUps: string[];
  rubric: AdminRubricItem[];
  practiceAvailable: boolean;
  usageCount: number;
  averageScorePercent: number | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminSortKey =
  | "displayNo"
  | "level"
  | "topic"
  | "title"
  | "followUpCount"
  | "rubricCount"
  | "usageCount"
  | "averageScorePercent";

export type AdminListParams = {
  q?: string;
  /** `question_topics.code`; "all" or empty means no filter. */
  topic?: string;
  /** `question_levels.code`; "all" or empty means no filter. */
  level?: string;
  sort?: AdminSortKey | "";
  dir?: "asc" | "desc";
  page: number;
  pageSize: number;
};

/** Body of create (#21) and update (#22): the whole form state, PUT replaces everything. */
export type AdminQuestionInput = {
  topicId: string;
  levelId: string;
  title: string;
  contentMarkdown: string | null;
  followUps: string[];
  rubric: AdminRubricItem[];
};

export type RetireResult = {
  retiredIds: string[];
  failedItems: { id: string; code: string }[];
};

/** One CSV row as the server wants it (DD 24.2); `line` is the physical line in the file, header = 1. */
export type ImportRowInput = {
  line: number;
  topic: string;
  level: string;
  question: string;
  rubric: { criterionCode: string; weightPercent: number }[];
  followUps?: string[];
  content?: string;
  suggestedApproach?: string[];
  coreKeywords?: string[];
  sampleAnswer?: string;
};

export type ImportSkipReason =
  | "missingField"
  | "unknownTopic"
  | "unknownLevel"
  | "badRubric"
  | "rubricSum"
  | "duplicate"
  | "invalidField";

export type ImportResult = {
  importedCount: number;
  skippedRows: { line: number; reasonCode: string; detail: string | null }[];
};

// ---------------------------------------------------------------------------------------------
// Wire shapes
// ---------------------------------------------------------------------------------------------

type RowDto = {
  id: string;
  displayNo: number;
  level: AdminQuestionRef;
  topic: AdminQuestionRef;
  title: string;
  followUpCount: number;
  rubricCount: number;
  usageCount: number;
  averageScorePercent: number | string | null;
};

type ListDto = {
  items: RowDto[];
  page: PageMeta;
  meta: { shown: number; total: number };
};

type DetailDto = {
  id: string;
  displayNo: number;
  topicId: string;
  levelId: string;
  title: string;
  contentMarkdown: string | null;
  followUpQuestions: string[];
  rubric: {
    criterionCode: string;
    description: string | null;
    weightPercent: number | string;
  }[];
  status: string;
  practiceAvailable: boolean;
  usage: { usageCount: number; averageScorePercent: number | string | null };
  createdAt: string;
  updatedAt: string;
};

type RetiredDto = {
  retiredIds: string[];
  failedItems: { id: string; errorCode: string }[];
};

type ImportedDto = {
  importedCount: number;
  skippedRows: { line: number; reasonCode: string; detail: string | null }[];
};

// Jackson sends BigDecimal as a JSON number, but a string must not break the screen.
const num = (value: number | string): number => Number(value);
const numOrNull = (value: number | string | null): number | null =>
  value === null ? null : Number(value);

export function toRow(dto: RowDto): AdminQuestionRow {
  return {
    id: dto.id,
    displayNo: dto.displayNo,
    level: { code: dto.level.code, displayName: dto.level.displayName },
    topic: { code: dto.topic.code, displayName: dto.topic.displayName },
    title: dto.title,
    followUpCount: dto.followUpCount,
    rubricCount: dto.rubricCount,
    usageCount: dto.usageCount,
    averageScorePercent: numOrNull(dto.averageScorePercent),
  };
}

export function toDetail(dto: DetailDto): AdminQuestionDetail {
  return {
    id: dto.id,
    displayNo: dto.displayNo,
    topicId: dto.topicId,
    levelId: dto.levelId,
    title: dto.title,
    contentMarkdown: dto.contentMarkdown,
    followUps: dto.followUpQuestions,
    rubric: dto.rubric.map((item) => ({
      criterionCode: item.criterionCode,
      weightPercent: num(item.weightPercent),
      description: item.description,
    })),
    practiceAvailable: dto.practiceAvailable,
    usageCount: dto.usage.usageCount,
    averageScorePercent: numOrNull(dto.usage.averageScorePercent),
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

const isSet = (value: string | undefined) =>
  value !== undefined && value !== "" && value !== "all";

/** #19. Only the parameters that carry a value are sent; the server reads them strictly (INB-V01). */
export async function realListAdminQuestions(
  params: AdminListParams,
): Promise<AdminQuestionList> {
  const query: Record<string, unknown> = {
    page: params.page,
    pageSize: params.pageSize,
  };
  if (params.q) query.q = params.q;
  if (isSet(params.topic)) query.topic = params.topic;
  if (isSet(params.level)) query.level = params.level;
  if (params.sort) {
    query.sort = params.sort;
    query.dir = params.dir ?? "asc";
  }
  const dto = await api.get<ListDto>(BASE, query);
  return {
    items: dto.items.map(toRow),
    page: dto.page,
    total: dto.meta.total,
  };
}

/** #20. A retired, unknown or malformed id is 404 INB-010 (rethrown). */
export async function realGetAdminQuestion(
  id: string,
): Promise<AdminQuestionDetail> {
  return toDetail(
    await api.get<DetailDto>(`${BASE}/${encodeURIComponent(id)}`),
  );
}

function toBody(input: AdminQuestionInput) {
  return {
    topicId: input.topicId,
    levelId: input.levelId,
    title: input.title,
    contentMarkdown: input.contentMarkdown,
    followUpQuestions: input.followUps,
    rubric: input.rubric.map((item) => ({
      criterionCode: item.criterionCode,
      weightPercent: item.weightPercent,
      description: item.description,
    })),
  };
}

/** #21: 201 with the stored question. */
export async function realCreateAdminQuestion(
  input: AdminQuestionInput,
): Promise<AdminQuestionDetail> {
  return toDetail(await api.post<DetailDto>(BASE, toBody(input)));
}

/** #22: PUT replaces the form state; the CSV-only columns are untouched by the server. */
export async function realUpdateAdminQuestion(
  id: string,
  input: AdminQuestionInput,
): Promise<AdminQuestionDetail> {
  return toDetail(
    await api.put<DetailDto>(
      `${BASE}/${encodeURIComponent(id)}`,
      toBody(input),
    ),
  );
}

/** #23: 1 to 100 distinct ids; a partial success is still 200 with `failedItems`. */
export async function realRetireAdminQuestions(
  ids: readonly string[],
): Promise<RetireResult> {
  const dto = await api.post<RetiredDto>(`${BASE}/bulk/delete`, { ids });
  return {
    retiredIds: dto.retiredIds,
    failedItems: dto.failedItems.map((item) => ({
      id: item.id,
      code: item.errorCode,
    })),
  };
}

/** #24: 1 to 500 rows; the server re-checks every row and reports the skipped ones. */
export async function realImportAdminQuestions(
  rows: readonly ImportRowInput[],
): Promise<ImportResult> {
  const dto = await api.post<ImportedDto>(`${BASE}/import`, { rows });
  return {
    importedCount: dto.importedCount,
    skippedRows: dto.skippedRows.map((row) => ({
      line: row.line,
      reasonCode: row.reasonCode,
      detail: row.detail,
    })),
  };
}
