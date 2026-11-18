// State of the post-login overlay, kept outside React on purpose. Signing in changes the token,
// and `app/providers/query-provider.tsx` then remounts the whole screen subtree (so no data crosses
// sessions). Overlay state held in the screen's own `useState` was wiped by that remount, so the
// four steps never showed. Module-level, like `shared/lib/toast-store.ts`, it survives the remount.
"use client";

import { useSyncExternalStore } from "react";

type State = { step: number | null; path: string | null };

let state: State = { step: null, path: null };
const listeners = new Set<() => void>();

function commit(next: State) {
  state = next;
  for (const listener of listeners) listener();
}

export const authLoading = {
  /** Remembers the page the overlay started on, so it can be dropped once the visitor has left it. */
  setStep(step: number) {
    commit({ step, path: state.path ?? window.location.pathname });
  },
  clear() {
    if (state.step !== null) commit({ step: null, path: null });
  },
  clearIfLeft(pathname: string) {
    if (state.path !== null && state.path !== pathname) authLoading.clear();
  },
};

const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};

export function useAuthLoadingStep(): number | null {
  return useSyncExternalStore(
    subscribe,
    () => state.step,
    () => null,
  );
}
