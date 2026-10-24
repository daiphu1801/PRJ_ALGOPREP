// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// TanStack Query hooks for the class cluster, same withMockData boundary as
// entities/admin-dashboard/api/queries.ts — graduation only means deleting the __mock__ import.
import { useMockMutation, useMockQuery } from "@/shared/api";
import type { ClassForm } from "../model/types";
import {
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

const SPEC = "03-dd/api/F1_identity.md";
const STALE = 15_000;

export const useInstructorClasses = () =>
  useMockQuery({
    queryKey: ["classes", "list"],
    mock: listInstructorClasses,
    spec: SPEC,
    staleTime: STALE,
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
  useMockQuery({
    queryKey: ["classes", "invite-codes", classId],
    mock: () => listInviteCodes(classId),
    spec: SPEC,
    staleTime: STALE,
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

export const useCreateClass = () =>
  useMockMutation({
    mock: (form: ClassForm) => createClass(form),
    spec: SPEC,
    invalidate: INVALIDATE,
  });

export const useUpdateClass = () =>
  useMockMutation({
    mock: ({ id, form }: { id: string; form: ClassForm }) =>
      updateClass(id, form),
    spec: SPEC,
    invalidate: INVALIDATE,
  });

export const useDeleteClass = () =>
  useMockMutation({
    mock: (id: string) => deleteClass(id),
    spec: SPEC,
    invalidate: INVALIDATE,
  });

export const useCreateInviteCode = () =>
  useMockMutation({
    mock: (classId: string) => createInviteCode(classId),
    spec: SPEC,
    invalidate: INVALIDATE,
  });

export const useRemoveStudent = () =>
  useMockMutation({
    mock: (studentId: string) => removeStudent(studentId),
    spec: SPEC,
    invalidate: INVALIDATE,
  });

export const useMyClasses = () =>
  useMockQuery({
    queryKey: ["classes", "mine"],
    mock: listMyClasses,
    spec: SPEC,
    staleTime: STALE,
  });

/** One class of the learner, picked from the list (ponytail: no per-class endpoint in the mock). */
export const useMyClass = (classId: string) => {
  const query = useMyClasses();
  return { ...query, data: query.data?.find((k) => k.id === classId) };
};

export const useJoinClass = () =>
  useMockMutation({
    mock: (code: string) => joinClassByCode(code),
    spec: SPEC,
    invalidate: [["classes", "mine"]],
  });
