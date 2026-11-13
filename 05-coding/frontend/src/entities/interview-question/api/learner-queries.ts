// Data boundary of the learner side (USR0401, USR0402) over F6 #2 to #7. Mock mode runs the same
// hooks against `__mock__/learner-mock.ts`, which behaves like the server (filters, pages, keeps the
// per-user facts), so the screens carry one code path.
//
// Cache rules (03-dd/screens/users/USR0401 BR-SC-02): a rating or a bookmark PATCHES the cached list
// pages and the cached detail instead of refetching, and the list never refetches on its own, so a
// row the active filter would now drop stays visible until the learner changes a filter or reopens
// the screen.
"use client";

import {
  keepPreviousData,
  useInfiniteQuery,
  useMutation,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from "@tanstack/react-query";
import { ApiError, useMockQuery, withMockData } from "@/shared/api";
import type {
  DrillCard,
  LearnerQuestionDetail,
  LearnerQuestionFilters,
  LearnerQuestionItem,
  LearnerQuestionPage,
  PracticeAttempt,
  PracticeAttemptPage,
  QuestionRecall,
} from "../model/learner-types";
import type { RecallLevel } from "../model/types";
import {
  mockGetQuestion,
  mockListAttempts,
  mockListDrillCards,
  mockListQuestions,
  mockRateRecall,
  mockSetBookmark,
  mockSubmitPracticeAnswer,
} from "./__mock__/learner-mock";
import {
  DRILL_SIZE,
  realGetQuestion,
  realListDrillCards,
  realListQuestions,
  realRateRecall,
  realSetBookmark,
} from "./real-learner";
import { realListAttempts, realSubmitPracticeAnswer } from "./real-practice";

const SPEC = "03-dd/api/F6_interview-bank.md";
export const LEARNER_KEY = ["interview-question", "learner"] as const;
const LIST_KEY = [...LEARNER_KEY, "list"] as const;
const detailKey = (id: string) => [...LEARNER_KEY, "detail", id] as const;
const attemptsKey = (id: string) => [...LEARNER_KEY, "attempts", id] as const;

type ListData = InfiniteData<LearnerQuestionPage, number>;

/** One page of 20 at a time (F6 #2); the screen loads more through `fetchNextPage`. */
export function useLearnerQuestionList(filters: LearnerQuestionFilters) {
  return useInfiniteQuery({
    queryKey: [...LIST_KEY, filters],
    queryFn: ({ pageParam }) =>
      withMockData(
        () => mockListQuestions(filters, pageParam),
        () => realListQuestions(filters, pageParam),
      ),
    initialPageParam: 1,
    getNextPageParam: (last) =>
      last.number < last.totalPages ? last.number + 1 : undefined,
    // Reloads happen on a filter change or when the screen is reopened, never by themselves.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnMount: "always",
    retry: false,
    placeholderData: keepPreviousData,
  });
}

/** `data` is `null` once the server says INB-010 (question gone). */
export function useLearnerQuestionDetail(id: string | null) {
  return useMockQuery<LearnerQuestionDetail | null>({
    queryKey: detailKey(id ?? ""),
    mock: () => mockGetQuestion(id ?? ""),
    real: () => realGetQuestion(id ?? ""),
    spec: SPEC,
    staleTime: 0,
    enabled: id !== null,
  });
}

function patchList(
  client: QueryClient,
  id: string,
  change: (item: LearnerQuestionItem) => LearnerQuestionItem,
) {
  client.setQueriesData<ListData>({ queryKey: LIST_KEY }, (data) =>
    data
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) =>
              item.id === id ? change(item) : item,
            ),
          })),
        }
      : data,
  );
}

function patchDetail(
  client: QueryClient,
  id: string,
  change: (detail: LearnerQuestionDetail) => LearnerQuestionDetail,
) {
  client.setQueryData<LearnerQuestionDetail | null>(detailKey(id), (detail) =>
    detail ? change(detail) : detail,
  );
}

/** INB-010 on any learner call: the question was retired meanwhile, so it leaves every cached list. */
export function dropQuestionFromCaches(client: QueryClient, id: string) {
  client.setQueriesData<ListData>({ queryKey: LIST_KEY }, (data) =>
    data
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            items: page.items.filter((item) => item.id !== id),
          })),
        }
      : data,
  );
  client.setQueryData(detailKey(id), null);
}

/** For a screen that learns, from a detail read, that the question is gone (INB-010). */
export function useDropUnavailableQuestion() {
  const client = useQueryClient();
  return (id: string) => dropQuestionFromCaches(client, id);
}

export const isQuestionGone = (error: unknown) =>
  error instanceof ApiError && error.code === "INB-010";

const isGone = isQuestionGone;

/** F6 #4. Not optimistic: the review date comes from the server. */
export function useRateRecall() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, level }: { id: string; level: RecallLevel }) =>
      withMockData(
        () => mockRateRecall(id, level),
        () => realRateRecall(id, level),
      ),
    onSuccess: (recall: QuestionRecall, { id }) => {
      patchList(client, id, (item) => ({ ...item, recall: recall.level }));
      patchDetail(client, id, (detail) => ({ ...detail, recall }));
    },
    onError: (error, { id }) => {
      if (isGone(error)) dropQuestionFromCaches(client, id);
    },
  });
}

/** F6 #5. Optimistic: flips at once, sends the wanted state, rolls back on failure. */
export function useSetBookmark() {
  const client = useQueryClient();
  return useMutation({
    // Bookmark calls run one after another, so two quick clicks cannot arrive out of order.
    scope: { id: "interview-bookmark" },
    mutationFn: ({ id, bookmarked }: { id: string; bookmarked: boolean }) =>
      withMockData(
        () => mockSetBookmark(id, bookmarked),
        () => realSetBookmark(id, bookmarked),
      ),
    onMutate: ({ id, bookmarked }) => {
      const lists = client.getQueriesData<ListData>({ queryKey: LIST_KEY });
      const detail = client.getQueryData<LearnerQuestionDetail | null>(
        detailKey(id),
      );
      patchList(client, id, (item) => ({ ...item, bookmarked }));
      patchDetail(client, id, (d) => ({ ...d, bookmarked }));
      return { lists, detail };
    },
    onError: (error, { id }, context) => {
      if (isGone(error)) {
        dropQuestionFromCaches(client, id);
        return;
      }
      context?.lists.forEach(([key, data]) => client.setQueryData(key, data));
      if (context?.detail !== undefined)
        client.setQueryData(detailKey(id), context.detail);
    },
  });
}

/** F6 #2 with `drill=true`: asked once on demand, the answer is the whole session. */
export function useStartDrill() {
  return useMutation({
    mutationFn: (filters: LearnerQuestionFilters): Promise<DrillCard[]> =>
      withMockData(
        () => mockListDrillCards(filters, DRILL_SIZE),
        () => realListDrillCards(filters),
      ),
  });
}

/** F6 #6. The answer text goes to the server as data and is never echoed into a prompt here. */
export function useSubmitPracticeAnswer(questionId: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (answerText: string): Promise<PracticeAttempt> =>
      withMockData(
        () => mockSubmitPracticeAnswer(questionId, answerText),
        () => realSubmitPracticeAnswer(questionId, answerText),
      ),
    // A lost connection may still have stored the attempt, so the history is re-read on any outcome.
    onSettled: () =>
      client.invalidateQueries({ queryKey: attemptsKey(questionId) }),
  });
}

/** F6 #7: newest first, 10 per page, `before` cursor. An empty id (question not found) reads nothing. */
export function usePracticeAttempts(questionId: string) {
  return useInfiniteQuery({
    enabled: questionId !== "",
    queryKey: attemptsKey(questionId),
    queryFn: ({ pageParam }): Promise<PracticeAttemptPage> =>
      withMockData(
        () => mockListAttempts(questionId, pageParam),
        () => realListAttempts(questionId, pageParam),
      ),
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (last) =>
      last.hasMore && last.items.length > 0
        ? Math.min(...last.items.map((a) => a.attemptNo))
        : undefined,
    retry: false,
    staleTime: 0,
  });
}
