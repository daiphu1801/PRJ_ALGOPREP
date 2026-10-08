// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// TanStack Query hooks for the class cluster, same withMockData boundary as
// entities/admin-dashboard/api/queries.ts — graduation only means deleting the __mock__ import.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import type { ClassForm } from "../model/types";
import {
  classScoreTrend,
  createClass,
  createInviteCode,
  deleteClass,
  getClassStudent,
  listClassAttention,
  listClassStudents,
  listInstructorClasses,
  listInviteCodes,
  removeStudent,
  updateClass,
} from "./__mock__/class-mocks";

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/identity.md chưa định nghĩa endpoint này",
  );
}

const QUERY_OPTIONS = { staleTime: 15_000, retry: false } as const;

export const useInstructorClasses = () =>
  useQuery({
    queryKey: ["classes", "list"],
    queryFn: () => withMockData(listInstructorClasses, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useClassStudents = (classId?: string) =>
  useQuery({
    queryKey: ["classes", "students", classId ?? "all"],
    queryFn: () =>
      withMockData(() => listClassStudents(classId), notImplemented),
    ...QUERY_OPTIONS,
  });

export const useClassStudentDetail = (studentId: string, classId: string) =>
  useQuery({
    queryKey: ["classes", "student-detail", classId, studentId],
    queryFn: () =>
      withMockData(() => getClassStudent(studentId, classId), notImplemented),
    ...QUERY_OPTIONS,
  });

export const useInviteCodes = (classId: string, enabled: boolean) =>
  useQuery({
    queryKey: ["classes", "invite-codes", classId],
    queryFn: () => withMockData(() => listInviteCodes(classId), notImplemented),
    enabled,
    ...QUERY_OPTIONS,
  });

export const useClassAttention = (classId?: string) =>
  useQuery({
    queryKey: ["classes", "attention", classId ?? "all"],
    queryFn: () =>
      withMockData(() => listClassAttention(classId), notImplemented),
    ...QUERY_OPTIONS,
  });

export const useClassScoreTrend = (classId?: string) =>
  useQuery({
    queryKey: ["classes", "score-trend", classId ?? "all"],
    queryFn: () => withMockData(() => classScoreTrend(classId), notImplemented),
    ...QUERY_OPTIONS,
  });

function useInvalidateClasses() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["classes"] });
}

export function useCreateClass() {
  const invalidate = useInvalidateClasses();
  return useMutation({
    mutationFn: (form: ClassForm) =>
      withMockData(() => createClass(form), notImplemented),
    onSuccess: invalidate,
  });
}

export function useUpdateClass() {
  const invalidate = useInvalidateClasses();
  return useMutation({
    mutationFn: ({ id, form }: { id: string; form: ClassForm }) =>
      withMockData(() => updateClass(id, form), notImplemented),
    onSuccess: invalidate,
  });
}

export function useDeleteClass() {
  const invalidate = useInvalidateClasses();
  return useMutation({
    mutationFn: (id: string) =>
      withMockData(() => deleteClass(id), notImplemented),
    onSuccess: invalidate,
  });
}

export function useCreateInviteCode() {
  const invalidate = useInvalidateClasses();
  return useMutation({
    mutationFn: (classId: string) =>
      withMockData(() => createInviteCode(classId), notImplemented),
    onSuccess: invalidate,
  });
}

export function useRemoveStudent() {
  const invalidate = useInvalidateClasses();
  return useMutation({
    mutationFn: (studentId: string) =>
      withMockData(() => removeStudent(studentId), notImplemented),
    onSuccess: invalidate,
  });
}
