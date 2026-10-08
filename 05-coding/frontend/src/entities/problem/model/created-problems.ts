// PROTOTYPE mock — in-memory only, lost on reload (the real list comes from `problems` rows).
//
// Problems created in this session through "new" or a duplicate (F2-16). The management list mock
// reads them so a saved copy shows up; the authoring mock writes them.
import { claimProblemForViewer } from "./mock-ownership";

export type CreatedProblem = {
  /** Display code, e.g. "#2001". */
  code: string;
  title: string;
  topic: string;
  difficulty: string;
  status: "draft" | "published";
  testcaseCount: number;
};

const created: CreatedProblem[] = [];

export const listCreatedProblems = (): readonly CreatedProblem[] => created;

/** Creates the row on first save (no `code`) or refreshes it on later saves; returns the code. */
export function upsertCreatedProblem(
  row: Omit<CreatedProblem, "code">,
  code?: string,
): string {
  const existing = code
    ? created.find((item) => item.code === code)
    : undefined;
  if (existing) {
    Object.assign(existing, row);
    return existing.code;
  }
  const next = `#${2001 + created.length}`;
  created.push({ ...row, code: next });
  claimProblemForViewer(next);
  return next;
}
