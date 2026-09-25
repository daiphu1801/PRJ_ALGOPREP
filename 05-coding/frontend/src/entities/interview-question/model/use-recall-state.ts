// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// Shared by both USR0401 (interview-bank-list) and USR0402 (interview-question-detail): F6-12
// self-rating and F6-03 bookmarking write the same two per-user, per-question facts, and both
// screens read/write them for the same `InterviewQuestion.code` — [SoT:
// 02-bd/screens/users/USR0402_interview_question_detail.md:310-313] calls this out explicitly as
// "reuse, not a new slice". No backend yet, so state lives in this hook instead of TanStack Query;
// each screen seeds it from `InterviewQuestion.defaultRecall` and keeps its own React state — this
// is per-mount, not cross-tab, and resets on refresh (acceptable for a prototype with no API).
"use client";

import { useCallback, useState } from "react";
import type { InterviewQuestion, RecallLevel } from "./types";

export type RecallMap = Record<string, RecallLevel | null>;
export type BookmarkMap = Record<string, boolean>;

function buildInitialRecall(questions: InterviewQuestion[]): RecallMap {
  return Object.fromEntries(questions.map((q) => [q.code, q.defaultRecall]));
}

function buildInitialBookmarks(questions: InterviewQuestion[]): BookmarkMap {
  return Object.fromEntries(questions.map((q) => [q.code, false]));
}

export function useRecallAndBookmarkState(questions: InterviewQuestion[]) {
  const [recall, setRecall] = useState<RecallMap>(() => buildInitialRecall(questions));
  const [bookmarks, setBookmarks] = useState<BookmarkMap>(() => buildInitialBookmarks(questions));

  const rate = useCallback((code: string, level: RecallLevel) => {
    setRecall((prev) => ({ ...prev, [code]: level }));
  }, []);

  const toggleBookmark = useCallback((code: string) => {
    setBookmarks((prev) => ({ ...prev, [code]: !prev[code] }));
  }, []);

  return { recall, bookmarks, rate, toggleBookmark };
}
