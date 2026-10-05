// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Problem-bank topics (`topics`, F2-02) are admin-managed data like the interview topics
// (DEC-2026-1001-admin-configurable-settings): ADMIN creates, renames and deletes them, INSTRUCTOR only
// picks. In-memory stand-in for the table; resets on reload. The seed keys equal the topic names the
// problem mocks already use, so existing rows keep resolving.
"use client";

import {
  createManagedListStore,
  labelOf,
  type ManagedItem,
  type ManagedListError,
} from "@/shared/lib/managed-list-store";

export type ProblemTopic = ManagedItem;

export type ProblemTopicError = ManagedListError;

const store = createManagedListStore<ProblemTopic>(
  ["Array", "DP", "Graph", "Tree", "Greedy", "Stack", "String"].map((name) => ({
    key: name,
    label: name,
  })),
  "ptopic",
);

export const useProblemTopics = store.use;
export const addProblemTopic = (label: string) => store.add(label, {});
export const renameProblemTopic = (key: string, label: string) => store.update(key, { label });
export const moveProblemTopic = store.move;
export const removeProblemTopic = store.remove;
export const problemTopicLabel = labelOf;
