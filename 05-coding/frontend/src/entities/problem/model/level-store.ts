// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Problem difficulty levels (`problem_levels`, F2-02) are admin-managed data, kept apart from the interview
// question levels (DEC-2026-1001-admin-configurable-settings, round 6): ADMIN adds, renames, reorders and
// deletes them, INSTRUCTOR only picks. Difficulty carries no logic, it only classifies. In-memory stand-in
// for the table; resets on reload. Seed keys are the old enum values, upper-cased like the BD codes.
"use client";

import {
  createLevelListStore,
  type LevelItem,
} from "@/shared/lib/level-list-store";
import type { ManagedListError } from "@/shared/lib/managed-list-store";
import type { BadgeVariant } from "@/shared/ui";

export type ProblemLevel = LevelItem<BadgeVariant>;

export type ProblemLevelError = ManagedListError;

const store = createLevelListStore<BadgeVariant>(
  [
    { key: "EASY", label: "Dễ", tone: "success" },
    { key: "MEDIUM", label: "Trung bình", tone: "warn" },
    { key: "HARD", label: "Khó", tone: "negative" },
  ],
  "plevel",
  "neutral",
);

export const useProblemLevels = store.use;
export const addProblemLevel = store.add;
export const renameProblemLevel = store.rename;
export const moveProblemLevel = store.move;
export const removeProblemLevel = store.remove;
export const problemLevelLabel = store.label;
export const problemLevelTone = store.tone;
export const problemLevelRank = store.rank;
