// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Shared by both USR0401 (interview-bank-list) and USR0402 (interview-question-detail): F6-12
// self-rating and F6-03 bookmarking write the same two per-user, per-question facts, and both
// screens read/write them for the same `InterviewQuestion.code` — [SoT:
// 02-bd/screens/users/USR0402_interview_question_detail.md:310-313] calls this out explicitly as
// "reuse, not a new slice". No backend yet, so the facts live in a module-level store (user changes
// layered over `InterviewQuestion.defaultRecall`) that both screens read, so a rating made on the
// detail screen is still there on the list. It resets on reload.
"use client";

import { useCallback, useMemo, useSyncExternalStore } from "react";
import type { InterviewQuestion, RecallLevel } from "./types";

export type RecallMap = Record<string, RecallLevel | null>;
export type BookmarkMap = Record<string, boolean>;

type Overrides = { recall: RecallMap; bookmarks: BookmarkMap };

let state: Overrides = { recall: {}, bookmarks: {} };
const listeners = new Set<() => void>();

function update(next: Overrides) {
  state = next;
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

const getSnapshot = () => state;

/** Test helper: drop every user change and go back to the seeded values. */
export function resetRecallAndBookmarkState() {
  update({ recall: {}, bookmarks: {} });
}

export function useRecallAndBookmarkState(questions: InterviewQuestion[]) {
  const overrides = useSyncExternalStore(subscribe, getSnapshot, getSnapshot);

  const recall = useMemo<RecallMap>(
    () =>
      Object.fromEntries(
        questions.map((q) => [
          q.code,
          q.code in overrides.recall
            ? overrides.recall[q.code]!
            : q.defaultRecall,
        ]),
      ),
    [questions, overrides.recall],
  );
  const bookmarks = useMemo<BookmarkMap>(
    () =>
      Object.fromEntries(
        questions.map((q) => [q.code, overrides.bookmarks[q.code] ?? false]),
      ),
    [questions, overrides.bookmarks],
  );

  const rate = useCallback((code: string, level: RecallLevel) => {
    update({ ...state, recall: { ...state.recall, [code]: level } });
  }, []);

  const toggleBookmark = useCallback((code: string) => {
    update({
      ...state,
      bookmarks: { ...state.bookmarks, [code]: !state.bookmarks[code] },
    });
  }, []);

  return { recall, bookmarks, rate, toggleBookmark };
}
