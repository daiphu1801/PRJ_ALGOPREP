// Data boundary of the learner's problem list (USR0101): four independent reads and the random pick.
// Mock and real share one contract; a failing side block never takes the table down with it.
import { useMockMutation, useMockQuery } from "@/shared/api";
import {
  mockCatalog,
  mockMyClassAssignments,
  mockMyInProgress,
  mockRandomProblem,
  mockSuggestedProblems,
  mockTopicProgress,
} from "./__mock__/student-catalog-mocks";
import {
  realListCatalog,
  realMyClassAssignments,
  realMyClassProblems,
  realMyInProgress,
  realRandomProblem,
  realSuggestedProblems,
  realTopicProgress,
} from "./real-student-catalog";
import type {
  CatalogParams,
  ClassProblemsPage,
} from "../model/catalog-types";

const PROBLEM_BANK_SPEC = "03-dd/api/F2_problem-bank.md";
const JUDGE_SPEC = "03-dd/api/F4_judge-orchestration.md";

/** The "Bài đang làm dở" block shows at most this many cards (BD USR0101 Sheet 4.4). */
export const IN_PROGRESS_LIMIT = 3;

/** `ListPublishedProblems`. The previous page stays while the next loads, so a filter change does not blank the table. */
export function useProblemCatalog(params: CatalogParams) {
  return useMockQuery({
    queryKey: ["problem", "catalog", params],
    mock: () => mockCatalog(params),
    real: () => realListCatalog(params),
    spec: PROBLEM_BANK_SPEC,
    staleTime: 0,
    keepPrevious: true,
  });
}

/** `GetSuggestedProblems` (#12): the dashboard table; the server ranks and filters, the previous rows stay while a filter loads. */
export const useSuggestedProblems = (difficulty: string, topicId: string) =>
  useMockQuery({
    queryKey: ["problem", "suggested", difficulty, topicId],
    mock: () => mockSuggestedProblems(difficulty, topicId),
    real: () => realSuggestedProblems(difficulty, topicId),
    spec: PROBLEM_BANK_SPEC,
    staleTime: 0,
    keepPrevious: true,
  });

/** `GetRandomProblem`: the server picks inside the same filter; null means nothing matches. */
export const useRandomProblem = () =>
  useMockMutation<CatalogParams, string | null>({
    mock: mockRandomProblem,
    real: realRandomProblem,
    spec: PROBLEM_BANK_SPEC,
  });

/** `ListTopicsWithProgress`. */
export const useTopicProgress = () =>
  useMockQuery({
    queryKey: ["problem", "topic-progress"],
    mock: mockTopicProgress,
    real: realTopicProgress,
    spec: PROBLEM_BANK_SPEC,
  });

/** `ListMyClassAssignments`. */
export const useMyClassAssignments = () =>
  useMockQuery({
    queryKey: ["problem", "my-class-assignments"],
    mock: mockMyClassAssignments,
    real: realMyClassAssignments,
    spec: PROBLEM_BANK_SPEC,
  });

/**
 * `ListMyClassProblems` (#13A) for USR0702. Real data only: on mock data the class carries its own
 * assignments, so the screen leaves this query off (`enabled`) and it answers `null`.
 */
export const useMyClassProblems = (
  classId: string,
  page: number,
  enabled: boolean,
) =>
  useMockQuery<ClassProblemsPage | null>({
    queryKey: ["problem", "my-class-problems", classId, page],
    mock: () => null,
    real: () => realMyClassProblems(classId, page),
    spec: PROBLEM_BANK_SPEC,
    staleTime: 0,
    enabled,
    keepPrevious: true,
  });

/** `ListMyUnsolvedRecentProblems` (F4 #8). */
export const useMyInProgress = (limit: number = IN_PROGRESS_LIMIT) =>
  useMockQuery({
    queryKey: ["problem", "my-in-progress", limit],
    mock: () => mockMyInProgress(limit),
    real: () => realMyInProgress(limit),
    spec: JUDGE_SPEC,
  });
