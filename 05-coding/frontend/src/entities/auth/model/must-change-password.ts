"use client";

import { useSyncExternalStore } from "react";

// The login response says whether the account must pick a new password (an admin set a temporary
// one). The flag lives in sessionStorage so a reload does not skip the step; a new tab starts
// without it. The server does not block other endpoints (BD architecture F1 section 3), so this is
// a UI rule, and `GET /me/profile` (#9) is where a fresh tab will read it once that is built.
const KEY = "algoprep.mustChangePassword";
const listeners = new Set<() => void>();

function read(): boolean {
  try {
    return sessionStorage.getItem(KEY) === "1";
  } catch {
    return false;
  }
}

export function setMustChangePassword(value: boolean): void {
  try {
    if (value) sessionStorage.setItem(KEY, "1");
    else sessionStorage.removeItem(KEY);
  } catch {
    // Storage blocked: the flag then only lasts until the next login response.
  }
  listeners.forEach((listener) => listener());
}

export function useMustChangePassword(): boolean {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    read,
    () => false,
  );
}
