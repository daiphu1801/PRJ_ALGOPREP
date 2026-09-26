// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import { assignmentSummary, listClassAssignments, removeAssignmentFromClass } from "./__mock__/class-assignment-mocks";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/problem-bank.md chưa định nghĩa endpoint này");
}

const QUERY_OPTIONS = { staleTime: 15_000, retry: false } as const;

export const useClassAssignments = () =>
  useQuery({
    queryKey: ["class-assignments", "list"],
    queryFn: () => withMockData(listClassAssignments, notImplemented),
    ...QUERY_OPTIONS,
  });

export const useAssignmentSummary = () =>
  useQuery({
    queryKey: ["class-assignments", "summary"],
    queryFn: () => withMockData(assignmentSummary, notImplemented),
    ...QUERY_OPTIONS,
  });

export function useRemoveAssignment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ problemId, classId }: { problemId: string; classId: string }) =>
      withMockData(() => removeAssignmentFromClass(problemId, classId), notImplemented),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["class-assignments"] }),
  });
}
