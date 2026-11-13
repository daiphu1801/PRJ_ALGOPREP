// Data boundary of INS0202 (03-dd/api/F2_problem-bank.md #49 to #53): `withMockData(mock, real)`.
// `useClassAssignments()` (the whole unpaged list) serves INS0204 on mock data only; INS0101 reads #54.
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  ApiError,
  isMockMode,
  withMockData,
  type PageMeta,
} from "@/shared/api";
import {
  assignProblemsToClass,
  assignmentSummary,
  listAssignableProblems,
  listClassAssignments,
  removeAssignmentFromClass,
} from "./__mock__/class-assignment-mocks";
import {
  realAssign,
  realAssignmentSummary,
  realListAssignable,
  realListAssignments,
  realRemoveAssignment,
  realTopAssignedProblems,
  type AssignmentParams,
} from "./real-assignments";
import type {
  AssignableProblem,
  AssignedRow,
  AssignResult,
  TopAssignedProblem,
} from "../model/types";

export type { AssignmentParams };

const PAGE_SIZE = 20;

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/F2_problem-bank.md: danh sách không phân trang chưa có endpoint",
  );
}

const QUERY_OPTIONS = { staleTime: 15_000, retry: false } as const;

/**
 * Whole list, mock data only: INS0204 reads it in mock mode (see PROTOTYPE_DEBT). In real mode the
 * query is off, because no endpoint returns an unpaged list and INS0204 reads the paged #49.
 */
export const useClassAssignments = () =>
  useQuery({
    queryKey: ["class-assignments", "list"],
    queryFn: () => withMockData(listClassAssignments, notImplemented),
    enabled: isMockMode(),
    ...QUERY_OPTIONS,
  });

const TOP_COUNT = 5;

/** `ListMyAssignedProblems` (#54): top five by submissions in my classes, for INS0101. */
export const useTopAssignedProblems = () =>
  useQuery({
    queryKey: ["class-assignments", "top"],
    queryFn: () =>
      withMockData<TopAssignedProblem[]>(
        async () =>
          [...(await listClassAssignments())]
            .sort(
              (a, b) =>
                b.submissionCount - a.submissionCount ||
                a.title.localeCompare(b.title),
            )
            .slice(0, TOP_COUNT)
            .map(({ id, title, submissionCount }) => ({
              id,
              title,
              submissionCount,
            })),
        realTopAssignedProblems,
      ),
    ...QUERY_OPTIONS,
  });

function paged<T>(all: T[], page: number): { items: T[]; page: PageMeta } {
  return {
    items: all.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    page: {
      number: page,
      size: PAGE_SIZE,
      totalItems: all.length,
      totalPages: Math.max(1, Math.ceil(all.length / PAGE_SIZE)),
    },
  };
}

const matches = (title: string, q: string) =>
  !q.trim() || title.toLowerCase().includes(q.trim().toLowerCase());

/** `ListClassAssignments` (#49): one page, filtered by class and title on the server. */
export const useClassAssignmentsPage = (params: AssignmentParams) =>
  useQuery({
    queryKey: ["class-assignments", "page", params],
    queryFn: () =>
      withMockData(
        async () =>
          paged(
            (await listClassAssignments()).filter(
              (row) =>
                (params.classId === "all" ||
                  row.assignedClassIds.includes(params.classId)) &&
                matches(row.title, params.q),
            ),
            params.page,
          ),
        () => realListAssignments(params),
      ),
    placeholderData: keepPreviousData,
    ...QUERY_OPTIONS,
  });

/**
 * Every problem given to ONE class (INS0204 needs title, topic and AC rate for each row of #28), merged
 * from all pages of #49. ponytail: N sequential requests, one per 20 problems (each page also asks F4);
 * fine for a class of a few dozen problems. Add a by-id F2 read if a class ever carries hundreds.
 */
export const useClassAssignedProblems = (classId: string) =>
  useQuery({
    queryKey: ["class-assignments", "of-class", classId],
    queryFn: () =>
      withMockData<AssignedRow[]>(
        async () =>
          (await listClassAssignments()).filter((row) =>
            row.assignedClassIds.includes(classId),
          ),
        async () => {
          const first = await realListAssignments({ classId, q: "", page: 1 });
          const items = [...first.items];
          for (let page = 2; page <= first.page.totalPages; page += 1) {
            items.push(
              ...(await realListAssignments({ classId, q: "", page })).items,
            );
          }
          return items;
        },
      ),
    enabled: classId !== "",
    ...QUERY_OPTIONS,
  });

/** `GetInstructorAssignmentSummary` (#50). */
export const useAssignmentSummary = () =>
  useQuery({
    queryKey: ["class-assignments", "summary"],
    queryFn: () => withMockData(assignmentSummary, realAssignmentSummary),
    ...QUERY_OPTIONS,
  });

/** `RemoveClassAssignment` (#53). Settled, not success: PRB-025 means the list was stale and must reload. */
export function useRemoveAssignment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      problemId,
      classId,
    }: {
      problemId: string;
      classId: string;
    }) =>
      withMockData(
        () => removeAssignmentFromClass(problemId, classId),
        () => realRemoveAssignment(classId, problemId),
      ),
    onSettled: () =>
      queryClient.invalidateQueries({ queryKey: ["class-assignments"] }),
  });
}

/** `ListAssignableProblems` (#51). */
export const useAssignableProblems = (
  classId: string,
  q: string,
  page: number,
) =>
  useQuery({
    queryKey: ["class-assignments", "assignable", classId, q, page],
    queryFn: () =>
      withMockData<{ items: AssignableProblem[]; page: PageMeta }>(
        async () =>
          paged(
            (await listAssignableProblems(classId)).filter((row) =>
              matches(row.title, q),
            ),
            page,
          ),
        () => realListAssignable(classId, q, page),
      ),
    enabled: classId !== "",
    placeholderData: keepPreviousData,
    ...QUERY_OPTIONS,
  });

/** `AssignProblemsToClass` (#52). */
export function useAssignProblems() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      classId,
      problemIds,
    }: {
      classId: string;
      problemIds: string[];
    }) =>
      withMockData<AssignResult>(
        async () => {
          await assignProblemsToClass(classId, problemIds);
          return { succeeded: problemIds, skipped: [], failed: [] };
        },
        () => realAssign(classId, problemIds),
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["class-assignments"] }),
  });
}
