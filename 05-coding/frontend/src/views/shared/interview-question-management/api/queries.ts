// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the interview question list. Same shape as the problem list:
// `withMockData(mock, fetchReal)` switched by NEXT_PUBLIC_MOCK_DATA, `fetchReal` a NOT_IMPLEMENTED stub
// until 03-dd/api/interview-bank.md defines the list endpoint.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QuestionDraft } from "@/entities/interview-question";
import { ApiError, withMockData } from "@/shared/api";
import { importQuestions } from "./__mock__/import-questions";
import { loadInterviewQuestionPage } from "./__mock__/load-question-page";

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/interview-bank.md chưa định nghĩa endpoint này",
  );
}

export const useInterviewQuestionPage = () =>
  useQuery({
    queryKey: ["admin-interview-question-page"],
    queryFn: () => withMockData(loadInterviewQuestionPage, notImplemented),
    staleTime: 0,
    retry: false,
  });

/** Bulk-creates the validated rows of a CSV file, then refreshes the list. */
export function useImportQuestions() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (drafts: readonly QuestionDraft[]) =>
      withMockData(() => importQuestions(drafts), notImplemented),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["admin-interview-question-page"] }),
  });
}
