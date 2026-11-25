// Data boundary of SHR0201 (03-dd/api/F2_problem-bank.md #24 to #32). Mock and real share one contract:
// the server (or its mock) filters, sorts and pages, the screen only sends `AdminListParams`. The view
// passes no author: the server scopes A2 to their own problems from the token (BD SHR0201 Q1).
import { keepPreviousData, useQueries, useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { currentUserScope } from "@/shared/api/user-scope";
import { foldText, useProblemLevels } from "@/entities/problem";
import { useMockMutation, useMockQuery, withMockData } from "@/shared/api";
import {
  loadAdminProblemList,
  loadAttentionList,
  loadTopicDistribution,
  mockBulkDelete,
  mockBulkDifficulty,
  mockBulkPublish,
  mockBulkUnpublish,
  mockBulkTopic,
  mockExportFilteredProblems,
  mockExportProblems,
} from "./__mock__/admin-problem-mocks";
import {
  realAttentionList,
  realBulkDelete,
  realBulkDifficulty,
  realBulkPublish,
  realBulkUnpublish,
  realBulkTopic,
  realExportFilteredProblems,
  realExportProblems,
  realListAdminProblems,
  realTopicDistribution,
} from "./real-problem-management";
import type { AdminListParams } from "../model/types";

const SPEC = "03-dd/api/F2_problem-bank.md";

/** Prefix every list, block and bulk action shares; a write invalidates it as a whole. */
export const ADMIN_PROBLEMS_KEY = ["admin-problem-page"] as const;

/** `ListProblemsAdmin`. The previous page stays on screen while the next one loads, so a filter change does not blank the table. */
export function useAdminProblemList(params: AdminListParams) {
  const levels = useProblemLevels();
  const levelId = levels.find((level) => level.key === params.level)?.id;
  return useQuery({
    queryKey: [
      ...ADMIN_PROBLEMS_KEY,
      "list",
      currentUserScope(),
      params,
      levelId ?? null,
    ],
    queryFn: () =>
      withMockData(
        () => loadAdminProblemList(params),
        () => realListAdminProblems(params, levelId),
      ),
    placeholderData: (data, previous) =>
      previous?.queryKey[2] === currentUserScope()
        ? keepPreviousData(data)
        : undefined,
    staleTime: 0,
    retry: false,
  });
}

/** The server cuts a longer `q` (DD 25). */
const TITLE_QUERY_MAX = 100;

/**
 * Which of these titles the bank already has (F2-19 warning), folded. Not under `ADMIN_PROBLEMS_KEY`: every
 * publish of a run invalidates that prefix and would ask for all titles again. A failed look-up only means
 * no warning.
 */
export function useExistingTitles(
  titles: readonly string[],
): ReadonlySet<string> {
  const found = useQueries({
    queries: [...new Set(titles)].map((title) => {
      const params: AdminListParams = {
        q: title.slice(0, TITLE_QUERY_MAX),
        level: "all",
        status: "all",
        sort: "title",
        dir: "asc",
        page: 1,
        pageSize: 50,
      };
      return {
        queryKey: ["problem-title", currentUserScope(), title],
        queryFn: async () => {
          const list = await withMockData(
            () => loadAdminProblemList(params),
            () => realListAdminProblems(params, undefined),
          );
          return list.items.some(
            (item) => foldText(item.title) === foldText(title),
          )
            ? foldText(title)
            : null;
        },
        staleTime: 60_000,
        retry: false,
      };
    }),
    // A string, so the answer only changes identity when its content does (callers put it in effect deps).
    combine: joinFound,
  });
  return useMemo(() => new Set(found ? found.split(SEP) : []), [found]);
}

const SEP = String.fromCharCode(31);
const joinFound = (results: { data?: string | null }[]) =>
  results.flatMap(({ data }) => (data ? [data] : [])).join(SEP);

/** `GetTopicDistribution`: its own query, so a failure here leaves the table alone. */
export const useTopicDistribution = () =>
  useMockQuery({
    queryKey: [...ADMIN_PROBLEMS_KEY, "distribution", currentUserScope()],
    mock: loadTopicDistribution,
    real: realTopicDistribution,
    spec: SPEC,
    staleTime: 0,
  });

/** `GetAttentionList`. */
export const useAttentionList = () =>
  useMockQuery({
    queryKey: [...ADMIN_PROBLEMS_KEY, "attention", currentUserScope()],
    mock: loadAttentionList,
    real: realAttentionList,
    spec: SPEC,
    staleTime: 0,
  });

const refresh = [ADMIN_PROBLEMS_KEY] as const;

/** `PublishProblem` (bulk, 1 to 20). */
export const useBulkPublish = () =>
  useMockMutation({
    mock: (ids: string[]) => mockBulkPublish(ids),
    real: realBulkPublish,
    invalidate: refresh,
    spec: SPEC,
  });

/** `DeleteProblem` (bulk, soft delete). */
export const useBulkDelete = () =>
  useMockMutation({
    mock: (ids: string[]) => mockBulkDelete(ids),
    real: realBulkDelete,
    invalidate: refresh,
    spec: SPEC,
  });

type DifficultyVars = { ids: string[]; levelKey: string };

/** `ChangeDifficulty` (bulk). The level id comes from the catalogue the screen already shows. */
export function useBulkDifficulty() {
  const levels = useProblemLevels();
  return useMockMutation({
    mock: ({ ids, levelKey }: DifficultyVars) =>
      mockBulkDifficulty(ids, levelKey),
    real: ({ ids, levelKey }: DifficultyVars) =>
      realBulkDifficulty(
        ids,
        levels.find((level) => level.key === levelKey)?.id,
      ),
    invalidate: refresh,
    spec: SPEC,
  });
}

/** `ExportProblemsCsv` for the selected rows; the file is handed back to the caller to save. */
export const useExportProblems = () =>
  useMockMutation({
    mock: (ids: string[]) => mockExportProblems(ids),
    real: realExportProblems,
    spec: SPEC,
  });

export const useBulkUnpublish = () =>
  useMockMutation({
    mock: mockBulkUnpublish,
    real: realBulkUnpublish,
    invalidate: refresh,
    spec: SPEC,
  });
export const useBulkTopic = () =>
  useMockMutation({
    mock: ({ ids, topicId }: { ids: string[]; topicId: string }) =>
      mockBulkTopic(ids, topicId),
    real: ({ ids, topicId }: { ids: string[]; topicId: string }) =>
      realBulkTopic(ids, topicId),
    invalidate: refresh,
    spec: SPEC,
  });
export function useExportFilteredProblems() {
  const levels = useProblemLevels();
  return useMockMutation({
    mock: mockExportFilteredProblems,
    real: (params: AdminListParams) =>
      realExportFilteredProblems(
        params,
        levels.find((item) => item.key === params.level)?.id,
      ),
    spec: SPEC,
  });
}
