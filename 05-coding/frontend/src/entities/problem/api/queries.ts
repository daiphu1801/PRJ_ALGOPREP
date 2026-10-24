// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the problem-authoring screens (authoring, read-only detail, preview). Same shape
// as the other entities: `withMockData(mock, fetchReal)` switched by NEXT_PUBLIC_MOCK_DATA. Wiring
// the backend means filling the `notImplemented` slots with real calls; the views do not change.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  useMockMutation,
  useMockQuery,
  withMockData,
} from "@/shared/api";
import {
  generateTestcasesWithAi,
  loadProblemDraft,
  publishProblem,
  saveProblemDraft,
} from "./__mock__/problem-draft-mocks";
import {
  fetchProblemDetail,
  fetchProblemListPage,
  fetchSavedProblems,
  simulateSampleRun,
  simulateSubmission,
} from "./__mock__/problem-mocks";
import type {
  GenerateTestcasesInput,
  ProblemDraft,
} from "../model/draft-types";
import type {
  ProblemDetail,
  SampleRunResult,
  SubmissionRunResult,
} from "../model/types";

const PROBLEM_BANK_SPEC = "03-dd/api/F2_problem-bank.md";
const JUDGE_SPEC = "03-dd/api/F4_judge-orchestration.md";

/** Catalogue page: topics, rows, in-progress and class assignments (`ListPublishedProblems` and siblings, F2 sections 2-5). */
export const useProblemListPage = () =>
  useMockQuery({
    queryKey: ["problem", "list-page"],
    mock: fetchProblemListPage,
    spec: PROBLEM_BANK_SPEC,
  });

/** `GetProblemForWorkspace` / `GetProblemOverview`. `null` = unknown or unpublished id (the screen shows "not found"). */
export const useProblemDetail = (problemId: string | undefined) =>
  useMockQuery<ProblemDetail | null>({
    queryKey: ["problem", "detail", problemId],
    mock: () => (problemId ? (fetchProblemDetail(problemId) ?? null) : null),
    spec: PROBLEM_BANK_SPEC,
    enabled: Boolean(problemId),
  });

/** `ListMySavedProblems` (F2 section 9). */
export const useSavedProblems = () =>
  useMockQuery({
    queryKey: ["problem", "saved"],
    mock: fetchSavedProblems,
    spec: PROBLEM_BANK_SPEC,
  });

type SampleRunVars = {
  samples: { input: string; expectedOutput: string }[];
  attempt: number;
};

/** `RunSampleTestcases` (F4 section 5). Synchronous call; the view keeps its own pacing delay. */
export const useRunSampleTestcases = () =>
  useMockMutation<SampleRunVars, SampleRunResult>({
    mock: ({ samples, attempt }) => simulateSampleRun(samples, attempt),
    spec: JUDGE_SPEC,
  });

type SubmitVars = { totalCount: number; attempt: number };

/** `CreateSubmission` (F4 endpoint table row 3). Realtime per-testcase flow is untouched. */
export const useCreateSubmission = () =>
  useMockMutation<SubmitVars, SubmissionRunResult>({
    mock: ({ totalCount, attempt }) => simulateSubmission(totalCount, attempt),
    spec: JUDGE_SPEC,
  });

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/F2_problem-bank.md chưa định nghĩa endpoint này",
  );
}

const draftKey = (problemId?: string, fromId?: string) =>
  ["problem-draft", problemId ?? "new", fromId ?? null] as const;

/** `GetProblemForAuthoring`. Stale at once: an editor opened after a save must see that save. */
export const useProblemDraft = (problemId?: string, fromId?: string) =>
  useQuery({
    queryKey: draftKey(problemId, fromId),
    queryFn: () =>
      withMockData(() => loadProblemDraft(problemId, fromId), notImplemented),
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
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}

/** `UpdateProblemPublishStatus` (saves, then publishes). */
export function usePublishProblem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ draft, problemId }: WriteVars) =>
      withMockData(() => publishProblem(draft, problemId), notImplemented),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}

/** `GenerateTestcasesWithAi` (F2-14). Returns unapproved drafts; the screen merges them in. A run uses one of the per-problem attempts, so the record is refetched. */
export function useGenerateTestcases() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: GenerateTestcasesInput) =>
      withMockData(() => generateTestcasesWithAi(input), notImplemented),
    onSettled: () =>
      queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}
