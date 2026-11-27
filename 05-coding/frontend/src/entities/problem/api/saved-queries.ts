import { currentUserScope } from "@/shared/api/user-scope";
// Data boundary of the saved-problems list (USR0103, 03-dd/api/F2_problem-bank.md #8 to #10). Mock and
// real share one contract. A write refreshes the list by prefix, so the totals in the header stay right.
import { useMockMutation, useMockQuery } from "@/shared/api";
import {
  mockListSaved,
  mockRestore,
  mockSaveNote,
  mockUnsave,
} from "./__mock__/saved-problems-mocks";
import {
  realListSaved,
  realRestore,
  realSaveNote,
  realSetBookmarked,
  realUnsave,
} from "./real-bookmarks";
import type {
  RestoreInput,
  SaveNoteInput,
  SavedParams,
} from "../model/saved-types";

const SPEC = "03-dd/api/F2_problem-bank.md";
const SAVED_KEY = ["problem", "saved"] as const;
const BOOKMARK_READS = [
  SAVED_KEY,
  ["problem", "workspace"],
  ["problem", "detail"],
] as const;

/** `ListMySavedProblems`. The previous page stays while the next loads, so a filter change does not blank the table. */
export function useSavedProblemsPage(params: SavedParams) {
  return useMockQuery({
    queryKey: [...SAVED_KEY, currentUserScope(), params],
    mock: () => mockListSaved(params),
    real: () => realListSaved(params),
    spec: SPEC,
    staleTime: 0,
    keepPrevious: true,
    previousScopeIndex: 2,
  });
}

/** `RemoveMyBookmark`. */
export const useUnsaveProblem = () =>
  useMockMutation<string>({
    mock: mockUnsave,
    real: realUnsave,
    invalidate: BOOKMARK_READS,
    spec: SPEC,
  });

/** `SaveMyBookmark` as a note edit: never creates the bookmark again (PRB-014 when it is gone). */
export const useSaveBookmarkNote = () =>
  useMockMutation<SaveNoteInput>({
    mock: mockSaveNote,
    real: realSaveNote,
    invalidate: BOOKMARK_READS,
    spec: SPEC,
  });

/** `SaveMyBookmark` as "Hoàn tác": puts the bookmark back with its note. */
export const useRestoreBookmark = () =>
  useMockMutation<RestoreInput>({
    mock: mockRestore,
    real: realRestore,
    invalidate: BOOKMARK_READS,
    spec: SPEC,
  });

/** `ToggleMyProblemBookmark` (#11) for the bookmark button of USR0102 and USR0104; the mock just echoes the wanted state. */
export const useSetBookmark = () =>
  useMockMutation<{ problemId: string; bookmarked: boolean }, boolean>({
    mock: ({ bookmarked }) => bookmarked,
    real: realSetBookmarked,
    invalidate: BOOKMARK_READS,
    spec: SPEC,
  });
