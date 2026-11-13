// Mock side of the admin question API (SHR0301/0302/0303). It answers the same contract as
// `real-admin-questions.ts` (server-side filter, sort and paging, ids), built on the existing
// in-memory bank in `__mock__/interview-question-mocks.ts`. In mock mode the id of a question is its
// code ("IQ-014"), the level and topic refs carry the stable key as both code and name.
import type { QuestionDraft } from "../model/csv-import";
import type { InterviewQuestion } from "../model/types";
import {
  appendImportedQuestions,
  fetchInterviewQuestionPage,
  findInterviewQuestionByCode,
} from "./__mock__/interview-question-mocks";
import type {
  AdminListParams,
  AdminQuestionDetail,
  AdminQuestionInput,
  AdminQuestionList,
  AdminQuestionRow,
  ImportResult,
  ImportRowInput,
  RetireResult,
} from "./real-admin-questions";

const MOCK_LATENCY_MS = 150;
const wait = () =>
  new Promise((resolve) => setTimeout(resolve, MOCK_LATENCY_MS));

// Questions retired in this visit; the bank itself is never mutated by a delete.
const retired = new Set<string>();

const displayNo = (code: string) => Number(code.replace(/\D/g, "")) || 0;
const LEVEL_ORDER = ["EASY", "MEDIUM", "HARD"];

// The mock keeps scores out of 5; the contract is a percentage and null before any attempt.
const toPercent = (score: number) =>
  score > 0 ? Math.round(score * 20) : null;

function toRow(question: InterviewQuestion): AdminQuestionRow {
  return {
    id: question.code,
    displayNo: displayNo(question.code),
    level: { code: question.level, displayName: question.level },
    topic: { code: question.topic, displayName: question.topic },
    title: question.question,
    followUpCount: question.followUps.length,
    rubricCount: question.rubric.length,
    usageCount: question.usageCount,
    averageScorePercent: toPercent(question.averageScore),
  };
}

function toDetail(question: InterviewQuestion): AdminQuestionDetail {
  return {
    id: question.code,
    displayNo: displayNo(question.code),
    topicId: question.topic,
    levelId: question.level,
    title: question.question,
    contentMarkdown: question.content || null,
    followUps: question.followUps,
    rubric: question.rubric.map((item) => ({
      criterionCode: item.label,
      weightPercent: item.weight,
      description: null,
    })),
    practiceAvailable: question.hasRubric,
    usageCount: question.usageCount,
    averageScorePercent: toPercent(question.averageScore),
    createdAt: "2026-10-01T00:00:00Z",
    updatedAt: "2026-10-01T00:00:00Z",
  };
}

function sortValue(row: AdminQuestionRow, key: string): number | string {
  switch (key) {
    case "level": {
      const at = LEVEL_ORDER.indexOf(row.level.code);
      return at < 0 ? LEVEL_ORDER.length : at;
    }
    case "topic":
      return row.topic.code;
    case "title":
      return row.title;
    case "followUpCount":
      return row.followUpCount;
    case "rubricCount":
      return row.rubricCount;
    case "usageCount":
      return row.usageCount;
    case "averageScorePercent":
      return row.averageScorePercent ?? -1;
    default:
      return row.displayNo;
  }
}

export async function loadAdminList(
  params: AdminListParams,
): Promise<AdminQuestionList> {
  await wait();
  const all = fetchInterviewQuestionPage()
    .questions.filter((question) => !retired.has(question.code))
    .map(toRow);
  const needle = (params.q ?? "").trim().toLowerCase();
  const matches = all.filter(
    (row) =>
      (!params.topic ||
        params.topic === "all" ||
        row.topic.code === params.topic) &&
      (!params.level ||
        params.level === "all" ||
        row.level.code === params.level) &&
      (!needle ||
        row.title.toLowerCase().includes(needle) ||
        row.id.toLowerCase().includes(needle)),
  );
  const factor = params.dir === "desc" ? -1 : 1;
  const sorted = params.sort
    ? [...matches].sort((left, right) => {
        const a = sortValue(left, params.sort as string);
        const b = sortValue(right, params.sort as string);
        const order =
          typeof a === "number" && typeof b === "number"
            ? a - b
            : String(a).localeCompare(String(b), "vi");
        return order * factor || left.displayNo - right.displayNo;
      })
    : matches;
  const totalPages = Math.max(1, Math.ceil(sorted.length / params.pageSize));
  const number = Math.min(Math.max(1, params.page), totalPages);
  return {
    items: sorted.slice(
      (number - 1) * params.pageSize,
      number * params.pageSize,
    ),
    page: {
      number,
      size: params.pageSize,
      totalItems: sorted.length,
      totalPages,
    },
    total: all.length,
  };
}

/** Real mode answers 404 for an unknown id; the mock answers `null`, which the screens read as not found. */
export async function loadAdminDetail(
  id: string,
): Promise<AdminQuestionDetail | null> {
  await wait();
  const found = retired.has(id) ? undefined : findInterviewQuestionByCode(id);
  return found ? toDetail(found) : null;
}

export async function mockCreate(
  input: AdminQuestionInput,
): Promise<AdminQuestionDetail> {
  await wait();
  const [created] = appendImportedQuestions([
    {
      topic: input.topicId,
      level: input.levelId,
      question: input.title,
      followUps: input.followUps,
      rubric: input.rubric.map((item) => ({
        label: item.criterionCode,
        weight: item.weightPercent,
      })),
      content: input.contentMarkdown ?? undefined,
    },
  ]);
  return toDetail(created!);
}

// The in-memory bank has no edit path (it never had one); a save only answers like a successful one.
export async function mockUpdate(
  id: string,
  input: AdminQuestionInput,
): Promise<AdminQuestionDetail> {
  await wait();
  const found = findInterviewQuestionByCode(id);
  return {
    ...(found
      ? toDetail(found)
      : toDetail(findInterviewQuestionByCode("IQ-014")!)),
    id,
    title: input.title,
    followUps: input.followUps,
    rubric: input.rubric,
  };
}

export async function mockRetire(
  ids: readonly string[],
): Promise<RetireResult> {
  await wait();
  const result: RetireResult = { retiredIds: [], failedItems: [] };
  for (const id of ids) {
    if (retired.has(id) || !findInterviewQuestionByCode(id)) {
      result.failedItems.push({ id, code: "INB-010" });
    } else {
      retired.add(id);
      result.retiredIds.push(id);
    }
  }
  return result;
}

export async function mockImport(
  rows: readonly ImportRowInput[],
): Promise<ImportResult> {
  await wait();
  const drafts: QuestionDraft[] = rows.map((row) => ({
    topic: row.topic,
    level: row.level,
    question: row.question,
    followUps: row.followUps ?? [],
    rubric: row.rubric.map((item) => ({
      label: item.criterionCode,
      weight: item.weightPercent,
    })),
    content: row.content,
    suggestedApproach: row.suggestedApproach,
    coreKeywords: row.coreKeywords,
    sampleAnswerFramework: row.sampleAnswer,
  }));
  const created = appendImportedQuestions(drafts);
  return { importedCount: created.length, skippedRows: [] };
}

/** Questions per topic and level over the whole mock bank, for the manager dialogs' "in use" guard. */
export async function loadMockUsage(): Promise<{
  topics: Record<string, number>;
  levels: Record<string, number>;
}> {
  const usage = {
    topics: {} as Record<string, number>,
    levels: {} as Record<string, number>,
  };
  for (const question of fetchInterviewQuestionPage().questions) {
    if (retired.has(question.code)) continue;
    usage.topics[question.topic] = (usage.topics[question.topic] ?? 0) + 1;
    usage.levels[question.level] = (usage.levels[question.level] ?? 0) + 1;
  }
  return usage;
}
