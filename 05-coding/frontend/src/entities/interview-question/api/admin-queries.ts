// Data boundary of SHR0301 / SHR0302 / SHR0303 (03-dd/api/F6_interview-bank.md #19 to #24). Mock and
// real share one contract: the server (or its mock) filters, sorts and pages, the screens only send
// `AdminListParams`. Pessimistic: a write waits for the server, then invalidates by prefix.
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { isMockMode, isNotFound, withMockData } from "@/shared/api";
import { currentUserScope } from "@/shared/api/user-scope";
import {
  loadAdminDetail,
  loadAdminList,
  loadMockUsage,
  mockCreate,
  mockImport,
  mockRetire,
  mockUpdate,
} from "./admin-mock";
import {
  realCreateAdminQuestion,
  realGetAdminQuestion,
  realImportAdminQuestions,
  realListAdminQuestions,
  realRetireAdminQuestions,
  realUpdateAdminQuestion,
  type AdminListParams,
  type AdminQuestionDetail,
  type AdminQuestionInput,
  type ImportRowInput,
} from "./real-admin-questions";

/** Prefix of every admin read; a write invalidates it as a whole. */
export const ADMIN_QUESTIONS_KEY = ["admin-interview-question"] as const;
// The learner bank reads the same questions; a signed-in admin may have it cached too.
const LEARNER_KEYS = [
  ["interview-question-page"],
  ["interview-question"],
] as const;

/** `ListInterviewQuestionsAdmin`. The previous page stays on screen while the next one loads. */
export function useAdminQuestionList(params: AdminListParams) {
  return useQuery({
    queryKey: [...ADMIN_QUESTIONS_KEY, "list", currentUserScope(), params],
    queryFn: () =>
      withMockData(
        () => loadAdminList(params),
        () => realListAdminQuestions(params),
      ),
    placeholderData: keepPreviousData,
    staleTime: 0,
    retry: false,
  });
}

/**
 * `GetInterviewQuestionDetail`. Resolves to `null` for an unknown or retired id (real: 404 `INB-010`),
 * which the screens show as the not-found state; every other failure stays an error.
 */
export function useAdminQuestion(id: string, enabled = true) {
  return useQuery<AdminQuestionDetail | null>({
    queryKey: [...ADMIN_QUESTIONS_KEY, "detail", currentUserScope(), id],
    queryFn: () =>
      withMockData(
        () => loadAdminDetail(id),
        () =>
          realGetAdminQuestion(id).catch((error: unknown) => {
            if (isNotFound(error)) return null;
            throw error;
          }),
      ),
    staleTime: 0,
    retry: false,
    enabled,
  });
}

/**
 * Questions per topic and level, for the manager dialogs' "still in use" guard. Mock mode counts the
 * in-memory bank; real mode asks nothing, the topic and level lists carry the server count
 * (`questionCount`, `includeCounts=true`).
 */
export function useQuestionUsage() {
  return useQuery({
    queryKey: [...ADMIN_QUESTIONS_KEY, "usage", currentUserScope()],
    queryFn: loadMockUsage,
    enabled: isMockMode(),
    staleTime: 0,
  });
}

function useAfterWrite() {
  const queryClient = useQueryClient();
  return () =>
    Promise.all(
      [ADMIN_QUESTIONS_KEY, ...LEARNER_KEYS].map((queryKey) =>
        queryClient.invalidateQueries({ queryKey }),
      ),
    );
}

/** `CreateInterviewQuestion`. */
export function useCreateAdminQuestion() {
  const afterWrite = useAfterWrite();
  return useMutation({
    mutationFn: (input: AdminQuestionInput) =>
      withMockData(
        () => mockCreate(input),
        () => realCreateAdminQuestion(input),
      ),
    onSuccess: afterWrite,
  });
}

/** `UpdateInterviewQuestion`: the whole form state replaces the stored one. */
export function useUpdateAdminQuestion() {
  const afterWrite = useAfterWrite();
  return useMutation({
    mutationFn: (vars: { id: string; input: AdminQuestionInput }) =>
      withMockData(
        () => mockUpdate(vars.id, vars.input),
        () => realUpdateAdminQuestion(vars.id, vars.input),
      ),
    onSuccess: afterWrite,
  });
}

/** `DeleteInterviewQuestion` (retire): one or many ids, partial success comes back as `failedItems`. */
export function useRetireAdminQuestions() {
  const afterWrite = useAfterWrite();
  return useMutation({
    mutationFn: (ids: readonly string[]) =>
      withMockData(
        () => mockRetire(ids),
        () => realRetireAdminQuestions(ids),
      ),
    onSuccess: afterWrite,
  });
}

/** `ImportInterviewQuestions`. */
export function useImportAdminQuestions() {
  const afterWrite = useAfterWrite();
  return useMutation({
    mutationFn: (rows: readonly ImportRowInput[]) =>
      withMockData(
        () => mockImport(rows),
        () => realImportAdminQuestions(rows),
      ),
    onSuccess: afterWrite,
  });
}
