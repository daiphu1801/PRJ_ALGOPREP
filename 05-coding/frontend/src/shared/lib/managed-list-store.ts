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

export type ManagedListOptions = {
  /** New keys become an upper-case slug of the name (`VERY_HARD`), like the BD's `code`; default is a time-based key. */
  slugKeys?: boolean;
  /** `remove` refuses once the list is down to this many items. Default 0. */
  minItems?: number;
};

/** "Rất khó" -> "RAT_KHO": strip diacritics, upper-case, non-alphanumerics to underscores. */
export function slugify(label: string): string {
  return label
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/gi, "d")
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export function createManagedListStore<T extends ManagedItem>(
  seed: readonly T[],
  keyPrefix: string,
  { slugKeys = false, minItems = 0 }: ManagedListOptions = {},
) {
  let items: readonly T[] = seed;
  const listeners = new Set<() => void>();

  function commit(next: readonly T[]) {
    items = next;
    listeners.forEach((listener) => listener());
  }

  // Slug taken by another item gets _2, _3 ... like the BD's slug rule for `code`.
  function uniqueSlug(name: string): string {
    const base = slugify(name) || keyPrefix.toUpperCase();
    let key = base;
    for (let n = 2; items.some((item) => item.key === key); n += 1) key = `${base}_${n}`;
    return key;
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
      const key = slugKeys ? uniqueSlug(name) : `${keyPrefix}-${Date.now().toString(36)}`;
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

    /** Moves an item one place up (-1) or down (+1); ignored at either end. */
    move(key: string, delta: -1 | 1) {
      const from = items.findIndex((item) => item.key === key);
      const to = from + delta;
      if (from < 0 || to < 0 || to >= items.length) return;
      const next = [...items];
      [next[from], next[to]] = [next[to]!, next[from]!];
      commit(next);
    },

    /**
     * Caller must have checked the item is unused: the BD refuses deleting a referenced item. Returns
     * false, and changes nothing, when the list is already down to `minItems`.
     */
    remove(key: string): boolean {
      if (items.length <= minItems) return false;
      commit(items.filter((item) => item.key !== key));
      return true;
    },
  };
}

export function labelOf(list: readonly ManagedItem[], key: string): string {
  return list.find((item) => item.key === key)?.label ?? key;
}
