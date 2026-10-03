// The one feedback channel (DEC-2026-1003-toast-feedback-channel): operation results and validation
// errors are pushed here as plain strings and drawn by `shared/ui/feedback/toaster.tsx`. Module-level store
// like `managed-list-store`, so any hook, dialog or view can call `toast.success(...)` without a
// provider or a prop. Text arrives already translated — this file knows nothing about i18n.
"use client";

import { useSyncExternalStore } from "react";

export type ToastTone = "success" | "error" | "warning" | "info";

export type ToastItem = {
  id: number;
  tone: ToastTone;
  message: string;
  /** How long the card stays before it dismisses itself. */
  durationMs: number;
};

// Errors and warnings are read slower and are the ones a user cannot afford to miss.
const DURATION_MS: Record<ToastTone, number> = {
  success: 4000,
  info: 4000,
  error: 7000,
  warning: 7000,
};

export const MAX_VISIBLE_TOASTS = 4;

const EMPTY: readonly ToastItem[] = [];

let items: readonly ToastItem[] = EMPTY;
let nextId = 0;
const listeners = new Set<() => void>();

function commit(next: readonly ToastItem[]) {
  items = next;
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function push(tone: ToastTone, message: string): number {
  const text = message.trim();
  if (!text) return -1;

  // The same message again moves the existing card to the end under a new id: no duplicate stack,
  // and the new id restarts its dismiss timer.
  const repeated = items.find((item) => item.tone === tone && item.message === text);
  const id = ++nextId;
  const item: ToastItem = { id, tone, message: text, durationMs: DURATION_MS[tone] };
  commit([...items.filter((existing) => existing !== repeated), item].slice(-MAX_VISIBLE_TOASTS));
  return id;
}

export const toast = {
  success: (message: string) => push("success", message),
  error: (message: string) => push("error", message),
  warning: (message: string) => push("warning", message),
  info: (message: string) => push("info", message),
  dismiss(id: number) {
    if (items.some((item) => item.id === id)) commit(items.filter((item) => item.id !== id));
  },
  /** Drops every card. Tests and sign-out use it. */
  clear() {
    if (items.length > 0) commit(EMPTY);
  },
};

/**
 * One error toast per failed submit: the first non-empty message wins. A form with several bad
 * fields would otherwise fire a burst of cards; the fields themselves already show a red border.
 */
export function toastFirstError(messages: readonly (string | null | undefined | false)[]) {
  const first = messages.find((message): message is string => Boolean(message));
  if (first) toast.error(first);
}

/** Live list, oldest first (the newest card renders last, nearest the corner). */
export function useToasts(): readonly ToastItem[] {
  return useSyncExternalStore(
    subscribe,
    () => items,
    () => EMPTY,
  );
}
