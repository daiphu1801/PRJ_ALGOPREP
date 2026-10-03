// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the problem-authoring screens (authoring, read-only detail, preview). Same shape
// as the other entities: `withMockData(mock, fetchReal)` switched by NEXT_PUBLIC_MOCK_DATA. Wiring
// the backend means filling the `notImplemented` slots with real calls; the views do not change.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import {
  generateTestcasesWithAi,
  loadProblemDraft,
  publishProblem,
  saveProblemDraft,
} from "./__mock__/problem-draft-mocks";
import type { GenerateTestcasesInput, ProblemDraft } from "../model/draft-types";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/problem-bank.md chưa định nghĩa endpoint này");
}

const draftKey = (problemId?: string) => ["problem-draft", problemId ?? "new"] as const;

/** `GetProblemForAuthoring`. Stale at once: an editor opened after a save must see that save. */
export const useProblemDraft = (problemId?: string) =>
  useQuery({
    queryKey: draftKey(problemId),
    queryFn: () => withMockData(() => loadProblemDraft(problemId), notImplemented),
    staleTime: 0,
    retry: false,
  });

type WriteVars = { draft: ProblemDraft; problemId?: string };

/** `SaveProblemContent`. */
export function useSaveProblemDraft() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ draft, problemId }: WriteVars) =>
      withMockData(() => saveProblemDraft(draft, problemId), notImplemented),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}

/** `UpdateProblemPublishStatus` (saves, then publishes). */
export function usePublishProblem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ draft, problemId }: WriteVars) =>
      withMockData(() => publishProblem(draft, problemId), notImplemented),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}

/** `GenerateTestcasesWithAi` (F2-14). Returns unapproved drafts; the screen merges them in. A run uses one of the per-problem attempts, so the record is refetched. */
export function useGenerateTestcases() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: GenerateTestcasesInput) =>
      withMockData(() => generateTestcasesWithAi(input), notImplemented),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}
