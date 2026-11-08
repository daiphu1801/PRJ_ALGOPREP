// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the problem-authoring screens (authoring, read-only detail, preview). Same shape
// as the other entities: `withMockData(mock, fetchReal)` switched by NEXT_PUBLIC_MOCK_DATA. Wiring
// the backend means passing the real call; #48 (AI generation) is the one slot still on `notImplemented`.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchOverviewProblem } from "./workspace-queries";
import {
  runRealSamples,
  submitRealCode,
  type ExecutionInput,
} from "./real-execution";
import {
  ApiError,
  useMockMutation,
  useMockQuery,
  withMockData,
} from "@/shared/api";
import {
  previewBody,
  realLoadDraft,
  realPreviewSignature,
  realSaveDraft,
  type SaveResult,
} from "./real-authoring";
import { useDebounced } from "@/shared/lib/use-debounced";
import {
  generateTestcasesWithAi,
  loadProblemDraft,
  publishProblem,
  saveProblemDraft,
} from "./__mock__/problem-draft-mocks";
import {
  fetchProblemDetail,
  fetchProblemListPage,
  simulateSampleRun,
  simulateSubmission,
} from "./__mock__/problem-mocks";
import type {
  FunctionSignature,
  GenerateTestcasesInput,
  ProblemDraft,
  ServerState,
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
    real: () => fetchOverviewProblem(problemId!),
    spec: PROBLEM_BANK_SPEC,
    staleTime: 0,
    enabled: Boolean(problemId),
  });

type SampleRunVars = ExecutionInput & {
  samples: { input: string; expectedOutput: string }[];
  attempt: number;
};

/** `RunSampleTestcases` (F4 section 5), with source and model sent to the real engine. */
export const useRunSampleTestcases = () =>
  useMockMutation<SampleRunVars, SampleRunResult>({
    mock: ({ samples, attempt }) => simulateSampleRun(samples, attempt),
    real: runRealSamples,
    spec: JUDGE_SPEC,
  });

type SubmitVars = ExecutionInput & { totalCount: number; attempt: number };

/** `CreateSubmission` returns an admission ticket; the workspace follows its progress separately. */
export const useCreateSubmission = () =>
  useMockMutation<SubmitVars, SubmissionRunResult>({
    mock: ({ totalCount, attempt }) => simulateSubmission(totalCount, attempt),
    real: submitRealCode,
    invalidate: [["submission"], ["problem", "workspace"]],
    spec: JUDGE_SPEC,
  });

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/F2_problem-bank.md: #48 (sinh testcase bằng AI) chưa có endpoint",
  );
}

const draftKey = (problemId?: string, fromId?: string) =>
  ["problem-draft", problemId ?? "new", fromId ?? null] as const;

/**
 * `GetProblemForAuthoring` (#33), `GetDuplicateSource` (#34) for a copy. Stale at once: an editor opened
 * after a save must see that save.
 */
export const useProblemDraft = (problemId?: string, fromId?: string) =>
  useQuery({
    queryKey: draftKey(problemId, fromId),
    queryFn: () =>
      withMockData(
        () => loadProblemDraft(problemId, fromId),
        () => realLoadDraft(problemId, fromId),
      ),
    staleTime: 0,
    retry: false,
  });

/**
 * What the screen sends to save. `saved`, `server` and `updatedAt` only matter in real mode: they tell
 * the sequence which sections changed and which lock value to send.
 */
export type WriteVars = {
  draft: ProblemDraft;
  problemId?: string;
  saved?: ProblemDraft;
  server?: ServerState;
  updatedAt?: string | null;
};

const ALL_SECTIONS: SaveResult["applied"] = ["content", "spec", "ai", "status"];

function save(
  vars: WriteVars,
  mock: (
    draft: ProblemDraft,
    problemId?: string,
  ) => Promise<string | undefined>,
): Promise<SaveResult> {
  return withMockData<SaveResult>(
    async () => ({
      createdId: await mock(vars.draft, vars.problemId),
      applied: ALL_SECTIONS,
    }),
    () =>
      realSaveDraft({
        draft: vars.draft,
        saved: vars.saved ?? vars.draft,
        problemId: vars.problemId,
        server: vars.server!,
        updatedAt: vars.updatedAt ?? null,
      }),
  );
}

/** `SaveProblemContent` (#35, #36) and the spec, AI context and status steps that follow it (#37 to #39). */
export function useSaveProblemDraft() {
  return useMutation({
    mutationFn: (vars: WriteVars) => save(vars, saveProblemDraft),
  });
}

/** `UpdateProblemPublishStatus` (saves, then publishes); the screen sends the draft with `status: "published"`. */
export function usePublishProblem() {
  return useMutation({
    mutationFn: (vars: WriteVars) => save(vars, publishProblem),
  });
}

/** `GenerateTestcasesWithAi` (F2-14, #48: not built). Returns unapproved drafts; the screen merges them in. A run uses one of the per-problem attempts, so the record is refetched. */
export function useGenerateTestcases() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: GenerateTestcasesInput) =>
      withMockData(() => generateTestcasesWithAi(input), notImplemented),
    onSettled: () =>
      queryClient.invalidateQueries({ queryKey: ["problem-draft"] }),
  });
}

/**
 * The server's declaration lines for the signature being edited (F3 P6), so the author sees what
 * the starter code will contain after saving. Real mode only. The result belongs to one exact
 * draft (the query key is the body), so a stale answer is never shown for a newer draft; the caller
 * keeps the local sketch while this loads, when the signature is not declared, or when it fails.
 */
export function useSignaturePreview(signature: FunctionSignature) {
  const declared = signature.functionName.trim() !== "";
  const body = useDebounced(JSON.stringify(previewBody(signature)), 400);
  return useQuery({
    queryKey: ["problem", "signature-preview", body],
    queryFn: () => realPreviewSignature(JSON.parse(body)),
    enabled: declared,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  });
}
