// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import type { GradingStatusFilter } from "../model/types";
import {
  listManualGradingQueue,
  listPendingManualGradingTop,
  manualGradingStats,
  saveManualGrade,
} from "./__mock__/manual-grading-mocks";

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/ai-review.md chưa định nghĩa endpoint này",
  );
}

const QUERY_OPTIONS = { staleTime: 15_000, retry: false } as const;

export const useManualGradingQueue = (
  status: GradingStatusFilter,
  classId?: string,
  studentId?: string,
) =>
  useQuery({
    queryKey: [
      "manual-grading",
      "queue",
      status,
      classId ?? "all",
      studentId ?? "none",
    ],
    queryFn: () =>
      withMockData(
        () => listManualGradingQueue(status, classId, studentId),
        notImplemented,
      ),
    ...QUERY_OPTIONS,
  });

export const useManualGradingStats = () =>
  useQuery({
    queryKey: ["manual-grading", "stats"],
    queryFn: () => withMockData(manualGradingStats, notImplemented),
    ...QUERY_OPTIONS,
  });

export const usePendingManualGradingTop = (limit = 5) =>
  useQuery({
    queryKey: ["manual-grading", "pending-top", limit],
    queryFn: () =>
      withMockData(() => listPendingManualGradingTop(limit), notImplemented),
    ...QUERY_OPTIONS,
  });

export function useSaveManualGrade() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      score,
      comment,
    }: {
      id: string;
      score: number;
      comment: string;
    }) =>
      withMockData(() => saveManualGrade(id, score, comment), notImplemented),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["manual-grading"] }),
  });
}
