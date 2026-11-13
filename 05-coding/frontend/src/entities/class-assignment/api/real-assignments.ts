// Real calls behind INS0202, class assignments (03-dd/api/F2_problem-bank.md #49 to #53, DD sections
// 50 to 54) and the INS0101 top-5 block (#54, DD section 55). Lists are paged by the server (20 rows); the statistics come from the judge module and
// may be unknown (`null`), which the screen shows as "-", never as 0.
import { api, type PageMeta } from "@/shared/api";
import type {
  AssignableProblem,
  AssignedRow,
  AssignmentSummary,
  AssignResult,
  TopAssignedProblem,
} from "../model/types";

const BASE = "/problem-bank/instructor";

export type AssignmentParams = {
  /** A class id, or "all". */
  classId: string;
  q: string;
  page: number;
};

type AssignmentDto = {
  problemId: string;
  title: string;
  topicNames: string[];
  levelCode: string;
  assignedClasses: { classId: string; className: string }[];
  submissionCount: number | null;
  acRatePercent: number | null;
  withdrawn: boolean;
};

type AssignableDto = {
  problemId: string;
  title: string;
  levelCode: string;
  topicNames: string[];
};

const trimmed = (q: string) => (q.trim() ? { q: q.trim() } : {});

/** #49. The figures already belong to the class tab asked for, so no per-class table is built. */
export async function realListAssignments(
  params: AssignmentParams,
): Promise<{ items: AssignedRow[]; page: PageMeta }> {
  const dto = await api.get<{ items: AssignmentDto[]; page: PageMeta }>(
    `${BASE}/class-assignments`,
    {
      ...(params.classId !== "all" ? { classId: params.classId } : {}),
      ...trimmed(params.q),
      page: params.page,
    },
  );
  return {
    items: dto.items.map((item) => ({
      id: item.problemId,
      title: item.title,
      topic: item.topicNames[0] ?? "",
      topics: item.topicNames,
      difficulty: item.levelCode,
      assignedClassIds: item.assignedClasses.map((c) => c.classId),
      assignedClassNames: item.assignedClasses.map((c) => c.className),
      submissionCount: item.submissionCount,
      acRate: item.acRatePercent,
      withdrawn: item.withdrawn,
    })),
    page: dto.page,
  };
}

/** #50. `avgAcRatePercent` is null when no problem has a submission or the statistics are down. */
export async function realAssignmentSummary(): Promise<AssignmentSummary> {
  const dto = await api.get<{
    assignedProblemCount: number;
    classCount: number;
    avgAcRatePercent: number | null;
  }>(`${BASE}/class-assignments/summary`);
  return {
    assignedCount: dto.assignedProblemCount,
    classCount: dto.classCount,
    avgAcRate: dto.avgAcRatePercent,
  };
}

/** #51: published bank problems not yet given to the class, in `code` order. */
export async function realListAssignable(
  classId: string,
  q: string,
  page: number,
): Promise<{ items: AssignableProblem[]; page: PageMeta }> {
  const dto = await api.get<{ items: AssignableDto[]; page: PageMeta }>(
    `${BASE}/classes/${classId}/assignable-problems`,
    { ...trimmed(q), page },
  );
  return {
    items: dto.items.map((item) => ({
      id: item.problemId,
      title: item.title,
      topic: item.topicNames[0] ?? "",
      difficulty: item.levelCode,
    })),
    page: dto.page,
  };
}

/** #52: each problem is handled on its own, so a 200 can still carry failures. */
export async function realAssign(
  classId: string,
  problemIds: string[],
): Promise<AssignResult> {
  const dto = await api.post<{
    succeededIds: string[];
    skippedIds: string[];
    failedItems: { id: string; code: string }[];
  }>(`${BASE}/classes/${classId}/assignments`, { problemIds });
  return {
    succeeded: dto.succeededIds,
    skipped: dto.skippedIds,
    failed: dto.failedItems.map(({ id, code }) => ({ id, code })),
  };
}

/** #53: 204; PRB-025 when it was already removed (the list is then reloaded by the caller). */
export async function realRemoveAssignment(
  classId: string,
  problemId: string,
): Promise<void> {
  await api.delete(`${BASE}/classes/${classId}/assignments/${problemId}`);
}

/**
 * #54: the five problems with the most submissions inside the caller's classes. The server answers
 * `items = []` when the statistics source is down, so an empty list is the only degraded state.
 */
export async function realTopAssignedProblems(): Promise<TopAssignedProblem[]> {
  const dto = await api.get<{
    items: { problemId: string; title: string; submissionCount: number }[];
  }>(`${BASE}/assigned-problems/top`);
  return dto.items.map((item) => ({
    id: item.problemId,
    title: item.title,
    submissionCount: item.submissionCount,
  }));
}
