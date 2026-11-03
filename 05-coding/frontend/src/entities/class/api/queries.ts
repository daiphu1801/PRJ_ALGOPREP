// PROTOTYPE with a real API behind part of it (task 12, 03-dd/api/F1_identity.md #19 to #27, #25A,
// #25B). The hooks of INS0201, INS0205, USR0701 and USR0702 take a real call through `withMockData`;
// the rest of the cluster (progress, student detail, assignments, activity: F1 #28, #29, #56 to #59,
// all F4-dependent) still goes through `useMockMutation` / `useMockQuery` and fails with
// NOT_IMPLEMENTED in real mode.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, useMockQuery, withMockData } from "@/shared/api";
import type { ClassForm } from "../model/types";
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

export const useClassStudents = (classId?: string, includeLeft = false) =>
  useMockQuery({
    queryKey: ["classes", "students", classId ?? "all", includeLeft],
    mock: () => listClassStudents(classId, includeLeft),
    spec: SPEC,
    staleTime: STALE,
  });

export const useClassStudentDetail = (
  studentId: string,
  classId: string,
  includeLeft = false,
) =>
  useMockQuery({
    queryKey: ["classes", "student-detail", classId, studentId, includeLeft],
    mock: () => getClassStudent(studentId, classId, includeLeft),
    spec: SPEC,
    staleTime: STALE,
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
    queryKey: ["classes", "mine"],
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
    queryKey: ["classes", "mine", classId],
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
