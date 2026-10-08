// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Interview-question difficulty levels are admin-managed data like the topics
// (DEC-2026-1001-admin-configurable-settings): ADMIN adds, renames, reorders and deletes them,
// INSTRUCTOR only picks. In-memory stand-in for the `question_levels` table; resets on reload. Built on
// the shared factory; problems have their OWN list (entities/problem/model/level-store.ts). The seed keys
// are the old enum values, upper-cased like the BD codes, so the mock questions keep resolving.
"use client";

import {
  createLevelListStore,
  type LevelItem,
} from "@/shared/lib/level-list-store";
import type { ManagedListError } from "@/shared/lib/managed-list-store";
import type { BadgeVariant } from "@/shared/ui";

export type InterviewLevel = LevelItem<BadgeVariant>;

export type LevelMutationError = ManagedListError;

const store = createLevelListStore<BadgeVariant>(
  [
    { key: "EASY", label: "Dễ", tone: "success" },
    { key: "MEDIUM", label: "Trung bình", tone: "warn" },
    { key: "HARD", label: "Khó", tone: "negative" },
  ],
  "level",
  "neutral",
);

export const useInterviewLevels = store.use;
export const addInterviewLevel = store.add;
export const renameInterviewLevel = store.rename;
export const moveInterviewLevel = store.move;
export const removeInterviewLevel = store.remove;
export const levelLabel = store.label;
export const levelTone = store.tone;
