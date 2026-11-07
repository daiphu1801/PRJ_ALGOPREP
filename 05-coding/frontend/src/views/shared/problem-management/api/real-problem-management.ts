// Real calls of SHR0201 (03-dd/api/F2_problem-bank.md #24 to #32, DD sections 25 to 33). Levels and topics
// have their own calls in entities/problem/api/real-catalog.ts. Not wired, on purpose: #29 `unpublish`
// (the screen has no button for it, BD SHR0201 EVT-11 words one "Xuất bản" action) and #31 `topic`
// (the prototype has no topic picker yet, see PROTOTYPE_DEBT).
import { api, ApiError } from "@/shared/api";
import type {
  AdminListParams,
  AdminProblem,
  AdminProblemList,
  AttentionItem,
  AttentionRuleCode,
  BulkResult,
  ProblemSortKey,
  TopicDistribution,
} from "../model/types";

const BASE = "/problem-bank/authoring/problems";

type ProblemDto = {
  id: string;
  code: string;
  title: string;
  topicNames: string[];
  levelCode: string;
  status: "UNPUBLISHED" | "PUBLISHED";
  submissionCount: number;
  assignedClassCount: number;
  acRate: number | null;
  testcaseCount: number;
  updatedAt: string;
};

type ListDto = {
  items: ProblemDto[];
  page: {
    number: number;
    size: number;
    totalItems: number;
    totalPages: number;
  };
  totals: { total: number; published: number };
};

// The screen's sort keys are its column names; the server names the underlying fields.
const SORT_FIELD: Record<ProblemSortKey, string> = {
  code: "code",
  title: "title",
  difficulty: "level",
  status: "status",
  submissions: "submissionCount",
  accepted: "acRate",
  edited: "updatedAt",
};

const RULE: Record<string, AttentionRuleCode> = {
  NO_TESTCASE: "noTestcase",
  LOW_AC: "lowAcceptRate",
  HIDDEN_FROM_LEARNERS: "hiddenFromLearners",
  STALE_DRAFT: "staleDraft",
};

export function toAdminProblem(dto: ProblemDto): AdminProblem {
  return {
    id: dto.id,
    code: dto.code,
    title: dto.title,
    topics: dto.topicNames,
    difficulty: dto.levelCode,
    status: dto.status === "PUBLISHED" ? "published" : "draft",
    submissionCount: dto.submissionCount,
    assignedClassCount: dto.assignedClassCount,
    acceptedRate: dto.acRate,
    testcaseCount: dto.testcaseCount,
    updatedAt: dto.updatedAt,
  };
}

/** #24. The server reads the filters strictly (PRB-001), so only valid values are sent. */
export async function realListAdminProblems(
  params: AdminListParams,
  levelId: string | undefined,
): Promise<AdminProblemList> {
  const q = params.q.trim();
  const dto = await api.get<ListDto>(BASE, {
    ...(q ? { q } : {}),
    ...(params.level !== "all" && levelId ? { level: levelId } : {}),
    ...(params.status !== "all"
      ? { status: params.status === "published" ? "PUBLISHED" : "UNPUBLISHED" }
      : {}),
    sort: SORT_FIELD[params.sort],
    dir: params.dir,
    page: params.page,
    pageSize: params.pageSize,
  });
  return {
    items: dto.items.map(toAdminProblem),
    page: dto.page,
    totals: dto.totals,
  };
}

/** #25. */
export async function realTopicDistribution(): Promise<TopicDistribution> {
  const dto = await api.get<{
    totalProblemsWithTopic: number;
    totalTopicsUsed: number;
    items: { topicName: string; count: number; percent: number }[];
  }>(`${BASE}/topic-distribution`);
  return {
    totalProblemsWithTopic: dto.totalProblemsWithTopic,
    totalTopicsUsed: dto.totalTopicsUsed,
    items: dto.items.map(({ topicName, count, percent }) => ({
      topicName,
      count,
      percent,
    })),
  };
}

/** #26: always the four rules, in the server's order; a rule code this client does not know is dropped. */
export async function realAttentionList(): Promise<AttentionItem[]> {
  const { items } = await api.get<{
    items: { ruleCode: string; count: number }[];
  }>(`${BASE}/attention`);
  return items.flatMap(({ ruleCode, count }) => {
    const code = RULE[ruleCode];
    return code ? [{ ruleCode: code, count }] : [];
  });
}

type BulkDto = {
  succeededIds: string[];
  failedItems: { id: string; code: string }[];
};

async function bulk(
  action: string,
  body: Record<string, unknown>,
): Promise<BulkResult> {
  const dto = await api.post<BulkDto>(`${BASE}/bulk/${action}`, body);
  return {
    succeededIds: dto.succeededIds,
    failedItems: dto.failedItems.map(({ id, code }) => ({ id, code })),
  };
}

/** #28: 1 to 20 ids; PRB-030 (checklist not met) and PRB-050 (sandbox down) arrive per problem. */
export const realBulkPublish = (problemIds: string[]) =>
  bulk("publish", { problemIds });

/** #30: `levelId` unknown rejects the whole batch with PRB-039. */
export function realBulkDifficulty(
  problemIds: string[],
  levelId: string | undefined,
): Promise<BulkResult> {
  // A level the client cannot resolve means its list changed under it: same answer as the server's.
  if (!levelId) {
    return Promise.reject(new ApiError("PRB-039", 422, "level not in list"));
  }
  return bulk("difficulty", { problemIds, levelId });
}

/** #32: a soft delete; submissions, bookmarks and class assignments are kept. */
export const realBulkDelete = (problemIds: string[]) =>
  bulk("delete", { problemIds });

/** #27: exactly one of the selected ids or the whole filter; here the selection. */
export async function realExportProblems(
  problemIds: string[],
): Promise<{ blob: Blob; filename: string | null }> {
  return api.downloadPost(`${BASE}/export`, { problemIds });
}
