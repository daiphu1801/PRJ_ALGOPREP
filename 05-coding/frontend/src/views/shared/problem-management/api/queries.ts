// Data boundary of SHR0201 (03-dd/api/F2_problem-bank.md #24 to #32). Mock and real share one contract:
// the server (or its mock) filters, sorts and pages, the screen only sends `AdminListParams`. The view
// passes no author: the server scopes A2 to their own problems from the token (BD SHR0201 Q1).
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useProblemLevels } from "@/entities/problem";
import { useMockMutation, useMockQuery, withMockData } from "@/shared/api";
import {
  loadAdminProblemList,
  loadAttentionList,
  loadTopicDistribution,
  mockBulkDelete,
  mockBulkDifficulty,
  mockBulkPublish,
  mockExportProblems,
} from "./__mock__/admin-problem-mocks";
import {
  realAttentionList,
  realBulkDelete,
  realBulkDifficulty,
  realBulkPublish,
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
    queryKey: [...ADMIN_PROBLEMS_KEY, "list", params, levelId ?? null],
    queryFn: () =>
      withMockData(
        () => loadAdminProblemList(params),
        () => realListAdminProblems(params, levelId),
      ),
    placeholderData: keepPreviousData,
    staleTime: 0,
    retry: false,
  });
}

/** `GetTopicDistribution`: its own query, so a failure here leaves the table alone. */
export const useTopicDistribution = () =>
  useMockQuery({
    queryKey: [...ADMIN_PROBLEMS_KEY, "distribution"],
    mock: loadTopicDistribution,
    real: realTopicDistribution,
    spec: SPEC,
    staleTime: 0,
  });

/** `GetAttentionList`. */
export const useAttentionList = () =>
  useMockQuery({
    queryKey: [...ADMIN_PROBLEMS_KEY, "attention"],
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
