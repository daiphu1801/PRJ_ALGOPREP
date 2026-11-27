// PROTOTYPE mock of the learner's problem list (USR0101). It answers the same contract as the real
// endpoints (03-dd/api/F2_problem-bank.md #1 to #4): the "server" filters, sorts and pages, a page past
// the end answers the last page. The rows come from `problem-mocks.ts`, the same data the dashboard and
// the workspace home still read, so one problem looks the same everywhere in mock mode.
import { getProblemLevels, problemLevelRank } from "../../model/level-store";
import type {
  AssignmentGroup,
  CatalogPage,
  CatalogParams,
  CatalogRow,
  InProgressProblem,
  TopicProgress,
} from "../../model/catalog-types";
import { fetchProblemListPage } from "./problem-mocks";

const pause = () => new Promise((resolve) => setTimeout(resolve, 100));

function matching(params: CatalogParams): CatalogRow[] {
  const needle = params.q.trim().toLowerCase();
  return fetchProblemListPage()
    .items.map((item): CatalogRow => ({
      id: item.id,
      code: item.code,
      title: item.title,
      topics: item.topics,
      difficulty: item.difficulty,
      submissionModel: item.submissionModel,
      acRate: item.acRate,
      solveState: item.solveState,
      hasSolutionReview: item.hasSolutionReview,
    }))
    .filter(
      (row) =>
        (params.status === "all" || row.solveState === params.status) &&
        (params.difficulty === "all" || row.difficulty === params.difficulty) &&
        // A mock topic id is the lower-cased topic name ("array").
        (params.topicId === "all" ||
          row.topics.some((topic) => topic.toLowerCase() === params.topicId)) &&
        (!needle ||
          row.code.toLowerCase().includes(needle) ||
          row.title.toLowerCase().includes(needle)),
    );
}

/** The dashboard suggestions: unsolved first, at most seven, filtered like the server does (no ranking in mock mode). */
export async function mockSuggestedProblems(
  difficulty: string,
  topicId: string,
): Promise<CatalogRow[]> {
  await pause();
  return matching({
    q: "",
    status: "all",
    difficulty,
    topicId,
    sort: "difficulty",
    dir: "asc",
    page: 1,
    pageSize: 7,
  })
    .sort(
      (left, right) =>
        Number(left.solveState === "solved") -
        Number(right.solveState === "solved"),
    )
    .slice(0, 7);
}

export async function mockCatalog(params: CatalogParams): Promise<CatalogPage> {
  await pause();
  const factor = params.dir === "asc" ? 1 : -1;
  const levels = getProblemLevels();
  const rows = matching(params).sort((left, right) =>
    params.sort === "acRate"
      ? ((left.acRate ?? -1) - (right.acRate ?? -1)) * factor
      : (problemLevelRank(levels, left.difficulty) -
          problemLevelRank(levels, right.difficulty)) *
        factor,
  );
  const totalPages = Math.max(1, Math.ceil(rows.length / params.pageSize));
  const number = Math.min(Math.max(1, params.page), totalPages);
  return {
    items: rows.slice((number - 1) * params.pageSize, number * params.pageSize),
    page: {
      number,
      size: params.pageSize,
      totalItems: rows.length,
      totalPages,
    },
    solveStateAvailable: true,
  };
}

export async function mockRandomProblem(
  params: CatalogParams,
): Promise<string | null> {
  await pause();
  const rows = matching(params);
  return rows[Math.floor(Math.random() * rows.length)]?.id ?? null;
}

export async function mockTopicProgress(): Promise<TopicProgress> {
  await pause();
  return {
    items: fetchProblemListPage().topics.map((topic) => ({ ...topic })),
    solveStateAvailable: true,
  };
}

export async function mockMyClassAssignments(): Promise<AssignmentGroup[]> {
  await pause();
  return fetchProblemListPage()
    .classAssignments.slice(0, 3)
    .map((group, index) => ({
      classId: `class-${index + 1}`,
      className: group.className,
      items: group.items.map((item) => ({
        problemId: item.problemId,
        title: item.title,
        solveState: item.solveState,
        assignedAt: item.assignedAt,
      })),
    }));
}

export async function mockMyInProgress(
  limit: number,
): Promise<InProgressProblem[]> {
  await pause();
  return fetchProblemListPage().inProgress.slice(0, limit);
}
