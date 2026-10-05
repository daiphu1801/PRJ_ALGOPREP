// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Factory for an admin-managed list of difficulty levels (interview questions, problems —
// DEC-2026-1001-admin-configurable-settings). Each bounded context gets its OWN list built from this,
// since F2 and F6 keep separate tables. A level is a managed item plus a badge tone: seed levels keep
// their colours, one an admin adds takes `newTone`. Codes are upper-case slugs and the last level cannot
// be removed. In-memory stand-in for the table; resets on reload. The tone type is a parameter so this
// file does not import from `shared/ui`.
"use client";

import {
  createManagedListStore,
  labelOf,
  type ManagedItem,
  type ManagedListError,
} from "./managed-list-store";

export type LevelItem<Tone extends string> = ManagedItem & { tone: Tone };

export function createLevelListStore<Tone extends string>(
  seed: readonly LevelItem<Tone>[],
  keyPrefix: string,
  newTone: Tone,
) {
  const store = createManagedListStore<LevelItem<Tone>>(seed, keyPrefix, { slugKeys: true, minItems: 1 });

  return {
    use: store.use,
    add: (label: string): ManagedListError | null => store.add(label, { tone: newTone }),
    rename: (key: string, label: string) => store.update(key, { label }),
    move: store.move,
    remove: store.remove,
    label: (list: readonly LevelItem<Tone>[], key: string) => labelOf(list, key),
    tone: (list: readonly LevelItem<Tone>[], key: string): Tone =>
      list.find((item) => item.key === key)?.tone ?? newTone,
    /** Position in the admin-chosen order, for sorting by difficulty; unknown keys sort last. */
    rank: (list: readonly LevelItem<Tone>[], key: string): number => {
      const index = list.findIndex((item) => item.key === key);
      return index < 0 ? list.length : index;
    },
  };
}
