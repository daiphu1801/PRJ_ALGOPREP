// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// In-memory stand-in for a settings row ADMIN edits (thresholds, retention, limits —
// DEC-2026-1001-admin-configurable-settings). One module-level store per settings group, readable
// from any screen through `use()`. Resets on reload: the real thing is a table behind ADMIN-only
// endpoints.
"use client";

import { useSyncExternalStore } from "react";

export function createSettingsStore<T extends object>(initial: T) {
  let value = initial;
  const listeners = new Set<() => void>();

  function subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }

  return {
    use: (): T =>
      useSyncExternalStore(
        subscribe,
        () => value,
        () => initial,
      ),
    /** Current value outside React (a mock reading a setting another screen edits). */
    get: (): T => value,
    set(patch: Partial<T>) {
      value = { ...value, ...patch };
      listeners.forEach((listener) => listener());
    },
  };
}
