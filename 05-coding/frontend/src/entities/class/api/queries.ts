import { currentUserScope } from "@/shared/api/user-scope";
// PROTOTYPE with a real API behind most of it (03-dd/api/F1_identity.md #19 to #28, #25A, #25B, #56,
// #59, #45). The hooks of INS0201, INS0203, INS0204, INS0205, USR0701 and USR0702 take a real call
// through `withMockData`. What still goes through `useMockQuery` and fails with NOT_IMPLEMENTED in
// real mode: `useClassAttention` / `useClassScoreTrend` (#58, #57 are built, no screen uses them).
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { ApiError, isMockMode, useMockQuery, withMockData } from "@/shared/api";
import type { ClassForm, ClassStudentDetail } from "../model/types";
import {
  listClassRoster,
  classScoreTrend,
  createClass,
  createInviteCode,
  deleteClass,
  getClassStudent,
  joinClassByCode,
  listClassActivity,
  listClassAttention,
  listClassStudents,
  listInstructorClasses,
  listInviteCodes,
  listMyClasses,
  removeStudent,
  updateClass,
} from "./__mock__/class-mocks";
import {
  realCreateClass,
  realCreateInviteCode,
  realDeleteClass,
  realGetMyClass,
  realJoinClass,
  realLeaveClass,
  realListInstructorClasses,
  realListInviteCodes,
  realListMyClasses,
  realListRoster,
  realUpdateClass,
} from "./real-class";
import { realListClassActivity } from "./real-class-activity";
import {
  realGetClassStudent,
  realGetClassSummary,
  realListInstructorStudents,
  type StudentPageQuery,
} from "./real-class-metrics";

const SPEC = "03-dd/api/F1_identity.md";
const STALE = 15_000;

export const useInstructorClasses = () =>
  useQuery({
    queryKey: ["classes", "list"],
    queryFn: () =>
      withMockData(listInstructorClasses, realListInstructorClasses),
    staleTime: STALE,
    retry: false,
  });

/** The students of one class (F1 #26); the F4 figures ride along only in mock mode. */
export const useClassRoster = (classId: string) =>
  useQuery({
    queryKey: ["classes", "roster", classId],
    queryFn: () =>
      withMockData(
        () => listClassRoster(classId),
        () => realListRoster(classId),
      ),
    staleTime: STALE,
    retry: false,
  });

/**
 * Mock roster of every class including learners who left (INS0206 / INS0101 mock rows only). Real mode
 * never calls it: the left-class flag already rides on F4 #13 (`useInstructorSubmissionPage`), so the
 * query is off instead of failing with NOT_IMPLEMENTED.
 */
export const useClassStudents = (classId?: string, includeLeft = false) =>
  useMockQuery({
    enabled: isMockMode(),
    queryKey: ["classes", "students", classId ?? "all", includeLeft],
    mock: () => listClassStudents(classId, includeLeft),
    spec: SPEC,
    staleTime: STALE,
  });

/**
 * One student of a class (F1 #28); null when the class has no such active student. The mock can look
 * up a student who left (`includeLeft`); the real endpoint only knows the active ones.
 */
export const useClassStudentDetail = (
  studentId: string,
  classId: string,
  includeLeft = false,
) =>
  useQuery({
    queryKey: ["classes", "student-detail", classId, studentId, includeLeft],
    queryFn: () =>
      withMockData<ClassStudentDetail | null>(
        () => getClassStudent(studentId, classId, includeLeft),
        () => realGetClassStudent(classId, studentId),
      ),
    staleTime: STALE,
    retry: false,
  });

// One query feeds both hooks below, so #56 is fetched once per screen.
const classSummaryQuery = {
  queryKey: ["classes", "summary"],
  queryFn: () =>
    withMockData(
      async () => ({ items: await listInstructorClasses(), totals: null }),
      realGetClassSummary,
    ),
  staleTime: STALE,
  retry: false,
};

/** The class cards with completion, average and absent figures (F1 #56); INS0201. */
export const useInstructorClassSummary = () =>
  useQuery({ ...classSummaryQuery, select: (result) => result.items });

/** The distinct-people totals of #56 (students counted once); `null` in mock mode or on the #19 fallback. */
export const useInstructorClassTotals = () =>
  useQuery({ ...classSummaryQuery, select: (result) => result.totals });

/** One page of the instructor student list (F1 #59); INS0203. Search and paging are the server's. */
export const useInstructorStudentPage = (query: StudentPageQuery) =>
  useQuery({
    queryKey: [
      "classes",
      "students-page",
      query.classId ?? "all",
      query.q,
      query.page,
      query.pageSize,
    ],
    queryFn: () =>
      withMockData(
        async () => {
          const needle = query.q.trim().toLowerCase();
          const all = (await listClassStudents(query.classId)).filter(
            (s) => !needle || s.name.toLowerCase().includes(needle),
          );
          return {
            rows: all.slice(
              (query.page - 1) * query.pageSize,
              query.page * query.pageSize,
            ),
            total: all.length,
            totalPages: Math.ceil(all.length / query.pageSize),
          };
        },
        () => realListInstructorStudents(query),
      ),
    placeholderData: keepPreviousData,
    staleTime: STALE,
    retry: false,
  });

export const useInviteCodes = (classId: string, enabled: boolean) =>
  useQuery({
    queryKey: ["classes", "invite-codes", classId],
    queryFn: () =>
      withMockData(
        () => listInviteCodes(classId),
        () => realListInviteCodes(classId),
      ),
    staleTime: STALE,
    retry: false,
    enabled,
  });

export const useClassActivity = () =>
  useMockQuery({
    queryKey: ["classes", "activity"],
    mock: listClassActivity,
    real: realListClassActivity,
    spec: SPEC,
    staleTime: STALE,
  });

export const useClassAttention = (classId?: string) =>
  useMockQuery({
    queryKey: ["classes", "attention", classId ?? "all"],
    mock: () => listClassAttention(classId),
    spec: SPEC,
    staleTime: STALE,
  });

export const useClassScoreTrend = (classId?: string) =>
  useMockQuery({
    queryKey: ["classes", "score-trend", classId ?? "all"],
    mock: () => classScoreTrend(classId),
    spec: SPEC,
    staleTime: STALE,
  });

const INVALIDATE = [["classes"]] as const;

/** Every write refreshes the whole cluster by prefix: the server owns the counts and the lists. */
function useClassWrite<V, T>(
  mock: (variables: V) => Promise<T> | T,
  real: (variables: V) => Promise<T>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: V) =>
      withMockData(
        () => mock(variables),
        () => real(variables),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INVALIDATE[0] }),
  });
}

export const useCreateClass = () =>
  useClassWrite((form: ClassForm) => createClass(form), realCreateClass);

export const useUpdateClass = () =>
  useClassWrite(
    ({ id, form }: { id: string; form: ClassForm }) => updateClass(id, form),
    ({ id, form }) => realUpdateClass(id, form),
  );

export const useDeleteClass = () =>
  useClassWrite((id: string) => deleteClass(id), realDeleteClass);

export const useCreateInviteCode = () =>
  useClassWrite(
    (classId: string) => createInviteCode(classId),
    realCreateInviteCode,
  );

/** F1 #27: the learner leaves the class (the history stays); the mock only needs the learner. */
export const useRemoveStudent = () =>
  useClassWrite(
    ({ studentId }: { classId: string; studentId: string }) =>
      removeStudent(studentId),
    realLeaveClass,
  );

export const useMyClasses = () =>
  useQuery({
    queryKey: ["classes", "mine", currentUserScope()],
    queryFn: () => withMockData(listMyClasses, realListMyClasses),
    staleTime: STALE,
    retry: false,
  });

/**
 * One class of the learner (F1 #25B). The mock picks it from the list; a class that is not theirs, is
 * gone or was left answers IDT-411, which the screen shows as "not found", so it resolves to null.
 */
export const useMyClass = (classId: string) =>
  useQuery({
    queryKey: ["classes", "mine", currentUserScope(), classId],
    queryFn: () =>
      withMockData(
        async () =>
          (await listMyClasses()).find((k) => k.id === classId) ?? null,
        () =>
          realGetMyClass(classId).catch((error: unknown) => {
            if (error instanceof ApiError && error.code === "IDT-411") {
              return null;
            }
            throw error;
          }),
      ),
    staleTime: STALE,
    retry: false,
  });

export const useJoinClass = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) =>
      withMockData(
        () => joinClassByCode(code),
        () => realJoinClass(code),
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["classes", "mine"] }),
  });
};
