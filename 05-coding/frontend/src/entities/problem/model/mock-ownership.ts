// PROTOTYPE mock — no session or author column exists yet (BD SHR0201 Q1: `problems.author_id`).
//
// A2 (INSTRUCTOR) only sees and edits the problems they wrote; A3 (ADMIN) sees all. The real server
// decides this from the token, so the screens pass nothing. The prototype has no login identity, so the
// mock decides from the area the page is mounted under: `/instructor/...` is "the" instructor, whose
// problems are the codes below. Mock-only: it lives next to the data it filters and is read by the
// mocks, never by a view.
import { ApiError } from "@/shared/api";

/** Problem codes (without "#") written by the mock instructor. */
const INSTRUCTOR_OWNED = new Set(["121", "139", "200", "322", "1143", "1268", "1462", "987"]);

export function viewerIsInstructor(): boolean {
  try {
    return window.location.pathname.startsWith("/instructor");
  } catch {
    return false;
  }
}

/** Whether the current mock viewer may see this problem; `code` may carry the leading "#". */
export function canViewerSeeProblem(code: string): boolean {
  return !viewerIsInstructor() || INSTRUCTOR_OWNED.has(code.replace("#", ""));
}

/**
 * A2 opening someone else's problem gets the same 404 as a code that does not exist, so the answer
 * does not reveal which codes exist (BD SHR0203 Q8, decided 2026-10-03).
 */
export function assertViewerCanSeeProblem(code: string): void {
  if (!canViewerSeeProblem(code)) {
    throw new ApiError("PROBLEM_NOT_FOUND", 404, "Problem not found");
  }
}
