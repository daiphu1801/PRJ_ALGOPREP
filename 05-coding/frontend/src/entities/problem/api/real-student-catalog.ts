// Real calls behind the learner's problem list, USR0101 (03-dd/api/F2_problem-bank.md #1 to #4, DD sections
// 2 to 5) plus the in-progress block that belongs to F4 (03-dd/api/F4_judge-orchestration.md #8, section 11).
// Not asked for, and left out of the rows on purpose: the "has a Solution Review" badge (F5 is not built).
import { api, type PageMeta } from "@/shared/api";
import { overallVerdict } from "./real-execution";
import type {
  AssignmentGroup,
  CatalogPage,
  CatalogParams,
  CatalogRow,
  ClassProblemsPage,
  InProgressProblem,
  TopicProgress,
} from "../model/catalog-types";
import type { SolveState, SubmissionLanguage } from "../model/types";

const BASE = "/problem-bank";

// The server names the third state UNSOLVED, the screen has always called it "todo".
const SOLVE_STATE: Record<string, SolveState> = {
  SOLVED: "solved",
  ATTEMPTED: "attempted",
  UNSOLVED: "todo",
};
const SOLVE_STATE_PARAM: Record<SolveState, string> = {
  solved: "SOLVED",
  attempted: "ATTEMPTED",
  todo: "UNSOLVED",
};

/** null in, null out: an unknown solve state must stay unknown. */
export function toSolveState(
  value: string | null | undefined,
): SolveState | null {
  return value ? (SOLVE_STATE[value] ?? null) : null;
}

export const solveStateParam = (state: SolveState) => SOLVE_STATE_PARAM[state];

type ItemDto = {
  id: string;
  code: string;
  title: string;
  topicNames: string[];
  levelCode: string;
  acRate: number | null;
  functionWrapperSupported: boolean;
  solveState: string | null;
};

function filterParams(params: CatalogParams) {
  const q = params.q.trim();
  return {
    ...(q ? { q } : {}),
    ...(params.status !== "all"
      ? { status: SOLVE_STATE_PARAM[params.status] }
      : {}),
    ...(params.difficulty !== "all" ? { difficulty: params.difficulty } : {}),
    ...(params.topicId !== "all" ? { topic: params.topicId } : {}),
  };
}

function toRow(dto: ItemDto): CatalogRow {
  return {
    id: dto.id,
    code: dto.code,
    title: dto.title,
    topics: dto.topicNames,
    difficulty: dto.levelCode,
    submissionModel: dto.functionWrapperSupported ? "both" : "stdioOnly",
    acRate: dto.acRate,
    solveState: toSolveState(dto.solveState),
    hasSolutionReview: null,
  };
}

/** #1: every filter is read leniently by the server, so a value it does not know is ignored, not a 400. */
export async function realListCatalog(
  params: CatalogParams,
): Promise<CatalogPage> {
  const dto = await api.get<{
    items: ItemDto[];
    page: PageMeta;
    solveStateAvailable: boolean;
  }>(`${BASE}/problems`, {
    ...filterParams(params),
    sort: params.sort === "difficulty" ? "level" : "acRate",
    dir: params.dir,
    page: params.page,
    pageSize: params.pageSize,
  });
  return {
    items: dto.items.map(toRow),
    page: dto.page,
    solveStateAvailable: dto.solveStateAvailable,
  };
}

type SuggestedDto = {
  problemId: string;
  code: string;
  title: string;
  levelCode: string;
  topicName: string | null;
  acRate: number | null;
  solveState: string | null;
  functionWrapperSupported: boolean;
};

/**
 * #12 (`GET /suggested-problems`): at most seven rows, ranked and filtered by the server (DD section 13,
 * USR0601 BR-SC-07). `difficulty` and `topic` are optional; the ranking vector in the answer is not read here.
 */
export async function realSuggestedProblems(
  difficulty: string,
  topicId: string,
): Promise<CatalogRow[]> {
  const dto = await api.get<{ items: SuggestedDto[] }>(
    `${BASE}/suggested-problems`,
    {
      ...(difficulty !== "all" ? { difficulty } : {}),
      ...(topicId !== "all" ? { topic: topicId } : {}),
    },
  );
  return dto.items.map((item) => ({
    id: item.problemId,
    code: item.code,
    title: item.title,
    topics: item.topicName ? [item.topicName] : [],
    difficulty: item.levelCode,
    submissionModel: item.functionWrapperSupported ? "both" : "stdioOnly",
    acRate: item.acRate,
    solveState: toSolveState(item.solveState),
    hasSolutionReview: null,
  }));
}

/** #2: one id out of the same filter, picked by the server; null when nothing matches (not an error). */
export async function realRandomProblem(
  params: CatalogParams,
): Promise<string | null> {
  const dto = await api.get<{ problemId: string | null }>(
    `${BASE}/problems/random`,
    filterParams(params),
  );
  return dto.problemId;
}

/** #3: every topic with `solved` and `total`; `solved` is null while the solve state is unavailable. */
export async function realTopicProgress(): Promise<TopicProgress> {
  const dto = await api.get<{
    items: {
      topicId: string;
      topicName: string;
      solved: number | null;
      total: number;
    }[];
    solveStateAvailable: boolean;
  }>(`${BASE}/topics/progress`);
  return {
    items: dto.items.map((item) => ({
      id: item.topicId,
      name: item.topicName,
      solved: item.solved,
      total: item.total,
    })),
    solveStateAvailable: dto.solveStateAvailable,
  };
}

/** #4: at most three classes, newest assignment first. An empty list also covers "enrolment gate down". */
export async function realMyClassAssignments(): Promise<AssignmentGroup[]> {
  const { groups } = await api.get<{
    groups: {
      classId: string;
      className: string;
      items: {
        problemId: string;
        title: string;
        solveState: string | null;
        assignedAt: string;
      }[];
    }[];
  }>(`${BASE}/me/class-assignments`);
  return groups.map((group) => ({
    classId: group.classId,
    className: group.className,
    items: group.items.map((item) => ({
      problemId: item.problemId,
      title: item.title,
      solveState: toSolveState(item.solveState),
      assignedAt: item.assignedAt,
    })),
  }));
}

type InProgressDto = {
  problemId: string;
  problem: { title: string } | null;
  language: string;
  status: string;
  passedCount: number | null;
  totalCount: number | null;
};

const LANGUAGE: Record<string, SubmissionLanguage> = {
  JAVA: "java",
  CPP: "cpp",
  PYTHON: "python",
};

/**
 * F4 #8: the learner's latest unsolved attempts. A card whose problem the server could not name
 * (`problem` null, F2 unreachable) is dropped rather than drawn without a title or a working link.
 */
export async function realMyInProgress(
  limit: number,
): Promise<InProgressProblem[]> {
  const { items } = await api.get<{ items: InProgressDto[] }>(
    "/judge/me/in-progress-problems",
    { limit },
  );
  return items.flatMap((item) => {
    const language = LANGUAGE[item.language];
    const lastVerdict = overallVerdict[item.status];
    // A status or language this client does not know is dropped, not guessed.
    if (!item.problem || !language || !lastVerdict) return [];
    return [
      {
        problemId: item.problemId,
        title: item.problem.title,
        language,
        lastVerdict,
        passedCount: item.passedCount,
        totalCount: item.totalCount,
      },
    ];
  });
}

/**
 * #13A: every problem given to one class of the learner, withdrawn ones included, paged by 20. The
 * totals come from `meta` over all pages, so the progress is never counted from one page of rows.
 */
export async function realMyClassProblems(
  classId: string,
  page: number,
): Promise<ClassProblemsPage> {
  const dto = await api.get<{
    items: {
      classId: string;
      problemId: string;
      code: string;
      title: string;
      levelDisplayName: string;
      solveState: string;
      assignedAt: string;
      withdrawn: boolean;
    }[];
    page: PageMeta;
    meta: { totalAssigned: number; solvedCount: number | null };
  }>(`${BASE}/me/class-problems`, { classId, page });
  return {
    items: dto.items.flatMap((item) => {
      // A state this client does not know is dropped, not shown as "todo".
      const solveState = toSolveState(item.solveState);
      return solveState ? [{ ...item, solveState }] : [];
    }),
    page: dto.page,
    meta: dto.meta,
  };
}
