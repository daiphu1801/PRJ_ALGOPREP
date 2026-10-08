// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Rows-per-page choice remembered per browser (SHR0201 Q4, DEC-2026-1001-admin-configurable-settings:
// "nhớ theo tài khoản"). There is no account store yet, so localStorage stands in for it. Storage can
// throw or come back empty (private window, blocked site data): every access is guarded, and a
// module-level map keeps the choice for the rest of the visit. The server snapshot is `fallback`, so
// server and client markup agree on the first render.
"use client";

import { useSyncExternalStore } from "react";

const listeners = new Set<() => void>();
const visitChoice = new Map<string, number>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  window.addEventListener("storage", listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", listener);
  };
}

function read(
  storageKey: string,
  options: readonly number[],
  fallback: number,
): number {
  let stored: number | undefined = visitChoice.get(storageKey);
  try {
    const persisted = Number(localStorage.getItem(storageKey));
    if (persisted) stored = persisted;
  } catch {
    // Storage unavailable: use this visit's choice, if any.
  }
  return stored !== undefined && options.includes(stored) ? stored : fallback;
}

export function usePersistedPageSize(
  storageKey: string,
  options: readonly number[],
  fallback: number,
): [number, (size: number) => void] {
  const size = useSyncExternalStore(
    subscribe,
    () => read(storageKey, options, fallback),
    () => fallback,
  );

  function choose(next: number) {
    visitChoice.set(storageKey, next);
    try {
      localStorage.setItem(storageKey, String(next));
    } catch {
      // Not remembered across visits, still applied now.
    }
    listeners.forEach((listener) => listener());
  }

  return [size, choose];
}
