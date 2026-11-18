// PROTOTYPE mock of the saved-problems list (USR0103). It answers the same contract as the real endpoints
// (03-dd/api/F2_problem-bank.md #8 to #10): the "server" filters and pages (20 a page), newest saved first,
// and an unsave, a note edit and an undo change this in-memory list until the page is reloaded.
import { ApiError } from "@/shared/api";
import { fetchSavedProblems } from "./problem-mocks";
import type {
  RestoreInput,
  SaveNoteInput,
  SavedPage,
  SavedParams,
  SavedRow,
} from "../../model/saved-types";

export const SAVED_PAGE_SIZE = 20;

/** "20/09/2026" in the literal data becomes a timestamp the screen prints like a real one. */
function toIso(label: string): string {
  const [day, month, year] = label.split("/").map(Number);
  return new Date(Date.UTC(year!, month! - 1, day!, 5)).toISOString();
}

function seed(): SavedRow[] {
  return fetchSavedProblems().map((problem) => ({
    id: problem.id,
    code: problem.code,
    title: problem.title,
    difficulty: problem.difficulty,
    topics: problem.topics,
    solveState: problem.solveState,
    note: problem.note,
    savedAt: toIso(problem.savedAtLabel),
    withdrawn: problem.withdrawn ?? false,
  }));
}

let rows = seed();

/** Test hook: back to the literal data. */
export function resetSavedMock(): void {
  rows = seed();
}

const pause = () => new Promise((resolve) => setTimeout(resolve, 100));

export async function mockListSaved(params: SavedParams): Promise<SavedPage> {
  await pause();
  const needle = params.q.trim().toLowerCase();
  const matching = rows
    .filter(
      (row) =>
        (params.status === "all" || row.solveState === params.status) &&
        (params.difficulty === "all" || row.difficulty === params.difficulty) &&
        (!needle ||
          row.code.toLowerCase().includes(needle) ||
          row.title.toLowerCase().includes(needle)),
    )
    .sort((left, right) => right.savedAt.localeCompare(left.savedAt));
  const totalPages = Math.max(1, Math.ceil(matching.length / SAVED_PAGE_SIZE));
  const number = Math.min(Math.max(1, params.page), totalPages);
  return {
    items: matching
      .slice((number - 1) * SAVED_PAGE_SIZE, number * SAVED_PAGE_SIZE)
      .map((row) => ({ ...row })),
    page: {
      number,
      size: SAVED_PAGE_SIZE,
      totalItems: matching.length,
      totalPages,
    },
    meta: {
      savedTotal: rows.length,
      solvedCount: rows.filter((row) => row.solveState === "solved").length,
      filteredTotal: matching.length,
    },
  };
}

export async function mockUnsave(problemId: string): Promise<void> {
  await pause();
  // Idempotent, like the server: removing what is already gone is fine.
  rows = rows.filter((row) => row.id !== problemId);
}

export async function mockSaveNote(input: SaveNoteInput): Promise<void> {
  await pause();
  const found = rows.some((row) => row.id === input.problemId);
  if (!found) throw new ApiError("PRB-014", 404);
  rows = rows.map((row) =>
    row.id === input.problemId ? { ...row, note: input.note.trim() } : row,
  );
}

export async function mockRestore(input: RestoreInput): Promise<void> {
  await pause();
  if (rows.some((row) => row.id === input.problemId)) return;
  const original = seed().find((row) => row.id === input.problemId);
  if (!original) return;
  // Like the server, a problem withdrawn since cannot be bookmarked again.
  if (original.withdrawn) throw new ApiError("PRB-010", 404);
  // Like the server, a restore dates the bookmark from now.
  rows = [
    ...rows,
    { ...original, note: input.note, savedAt: new Date().toISOString() },
  ];
}
