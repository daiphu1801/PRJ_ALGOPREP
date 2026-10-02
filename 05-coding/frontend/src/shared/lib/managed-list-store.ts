// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// In-memory stand-in for an ADMIN-managed category table (interview topics, problem topics —
// DEC-2026-1001-admin-configurable-settings). One module-level store per list, readable from any
// screen through `use()`, so the admin list, the authoring form and the learner filter always see
// the same rows. Resets on reload: the real thing is a table behind ADMIN-only endpoints.
"use client";

import { useSyncExternalStore } from "react";

export type ManagedItem = {
  /** Stable key stored on the records that use the item. Never changes on rename. */
  key: string;
  /** Display name the admin typed. Plain data, not an i18n key. */
  label: string;
};

export type ManagedListError = "empty" | "duplicate";

const sameName = (left: string, right: string) =>
  left.trim().toLocaleLowerCase("vi") === right.trim().toLocaleLowerCase("vi");

export function createManagedListStore<T extends ManagedItem>(seed: readonly T[], keyPrefix: string) {
  let items: readonly T[] = seed;
  const listeners = new Set<() => void>();

  function commit(next: readonly T[]) {
    items = next;
    listeners.forEach((listener) => listener());
  }

  function subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }

  return {
    /** Live list in display order. */
    use: (): readonly T[] =>
      useSyncExternalStore(
        subscribe,
        () => items,
        () => seed,
      ),

    add(label: string, extra: Omit<T, keyof ManagedItem>): ManagedListError | null {
      const name = label.trim();
      if (!name) return "empty";
      if (items.some((item) => sameName(item.label, name))) return "duplicate";
      const key = `${keyPrefix}-${Date.now().toString(36)}`;
      commit([...items, { ...extra, key, label: name } as unknown as T]);
      return null;
    },

    /** Changes label and/or extra fields; the key never changes. */
    update(key: string, changes: Partial<Omit<T, "key">>): ManagedListError | null {
      const label = changes.label === undefined ? undefined : changes.label.trim();
      if (label !== undefined) {
        if (!label) return "empty";
        if (items.some((item) => item.key !== key && sameName(item.label, label))) return "duplicate";
      }
      commit(
        items.map((item) =>
          item.key === key ? { ...item, ...changes, ...(label === undefined ? {} : { label }) } : item,
        ),
      );
      return null;
    },

    /** Caller must have checked the item is unused: the BD refuses deleting a referenced item. */
    remove(key: string) {
      commit(items.filter((item) => item.key !== key));
    },
  };
}

export function labelOf(list: readonly ManagedItem[], key: string): string {
  return list.find((item) => item.key === key)?.label ?? key;
}
