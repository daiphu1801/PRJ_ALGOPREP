// Data boundary of INS0202 (03-dd/api/F2_problem-bank.md #49 to #53): `withMockData(mock, real)`.
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { withMockData, type PageMeta } from "@/shared/api";
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
  type AssignableParams,
  type AssignmentParams,
} from "./real-assignments";
import type {
  AssignableProblem,
  AssignedRow,
  AssignResult,
  TopAssignedProblem,
} from "../model/types";

export type { AssignableParams, AssignmentParams };

const PAGE_SIZE = 20;

const QUERY_OPTIONS = { staleTime: 15_000, retry: false } as const;

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

/** `ListAssignableProblems` (#51): search, level and topic filters are applied by the server. */
export const useAssignableProblems = (params: AssignableParams) =>
  useQuery({
    queryKey: ["class-assignments", "assignable", params],
    queryFn: () =>
      withMockData<{ items: AssignableProblem[]; page: PageMeta }>(
        async () =>
          paged(
            (await listAssignableProblems(params.classId)).filter(
              (row) =>
                matches(row.title, params.q) &&
                (params.level === "all" || row.difficulty === params.level) &&
                (params.topic === "all" ||
                  (row.topics ?? [row.topic]).includes(params.topic)),
            ),
            params.page,
          ),
        () => realListAssignable(params),
      ),
    enabled: params.classId !== "",
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
