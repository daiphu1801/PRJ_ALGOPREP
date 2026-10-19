// Browser-only workspace draft (USR0102 Q4, owner 2026-10-18): one entry per (problem, language,
// submission model). Shared by the workspace screen and by Solution Review's "apply to workspace".
import type { SubmissionLanguage } from "./types";

export type DraftModel = "function" | "stdio";

export function workspaceDraftKey(
  problemId: string,
  language: SubmissionLanguage,
  model: DraftModel,
): string {
  return `algoprep-draft:${problemId}:${language}:${model}`;
}

/** Returns false when storage is blocked or full, so the caller can tell the user. */
export function saveWorkspaceDraft(
  problemId: string,
  language: SubmissionLanguage,
  model: DraftModel,
  code: string,
): boolean {
  try {
    window.localStorage.setItem(
      workspaceDraftKey(problemId, language, model),
      code,
    );
    return true;
  } catch {
    return false;
  }
}
