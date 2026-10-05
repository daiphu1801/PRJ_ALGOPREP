// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Interview-question topics are admin-managed DATA, not a fixed enum (owner instruction 2026-10-01,
// DEC-2026-1001-admin-configurable-settings, which supersedes the "5 seeded topics" half of
// DEC-2026-0830-interview-bank-crud). The real thing is the `question_topics` table behind ADMIN-only
// endpoints; the shared in-memory store stands in for it so the admin list, the authoring form and
// the student bank filter all see one list. It resets on reload.
"use client";

import {
  createManagedListStore,
  labelOf,
  type ManagedItem,
  type ManagedListError,
} from "@/shared/lib/managed-list-store";

export type InterviewTopic = ManagedItem & {
  /**
   * Answers on this topic follow the STAR framework (Situation, Task, Action, Result). Replaces the
   * old hard link to the BEHAVIORAL code, so the admin decides which topics use it.
   */
  usesStarFramework: boolean;
};

export type TopicMutationError = ManagedListError;

// The five topics the bank started with; ordinary rows now, renamable and removable.
const store = createManagedListStore<InterviewTopic>(
  [
    { key: "csTheory", label: "Lý thuyết CS", usesStarFramework: false },
    { key: "systemDesign", label: "System design", usesStarFramework: false },
    { key: "database", label: "Database", usesStarFramework: false },
    { key: "language", label: "Ngôn ngữ", usesStarFramework: false },
    { key: "behavioural", label: "Hành vi", usesStarFramework: true },
  ],
  "topic",
);

export const useInterviewTopics = store.use;
export const addInterviewTopic = (label: string) => store.add(label, { usesStarFramework: false });
export const renameInterviewTopic = (key: string, label: string) => store.update(key, { label });
export const setInterviewTopicStar = (key: string, usesStarFramework: boolean) =>
  store.update(key, { usesStarFramework });
export const moveInterviewTopic = store.move;
export const removeInterviewTopic = store.remove;

export const topicLabel = labelOf;
