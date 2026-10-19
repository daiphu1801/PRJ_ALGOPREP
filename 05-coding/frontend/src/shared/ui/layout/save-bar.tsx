// Sticky bottom bar for a form that is saved as one batch: how many fields changed, an optional
// one-line note, and Discard / Save. Hidden while nothing has changed, so a clean form shows no bar.
//
// All strings arrive as props: shared/ui carries no i18n. `unsavedLabel` is already formatted
// ("3 thay đổi chưa lưu") because pluralisation and number format are the caller's locale.
"use client";

import type { ReactNode } from "react";
import { Button } from "../primitives/button";

type SaveBarProps = {
  dirtyCount: number;
  onSave: () => void;
  onDiscard: () => void;
  saving?: boolean;
  unsavedLabel: string;
  discardLabel: string;
  saveLabel: string;
  /** Static helper text next to the buttons, e.g. when the change takes effect. */
  note?: ReactNode;
};

export function SaveBar({
  dirtyCount,
  onSave,
  onDiscard,
  saving = false,
  unsavedLabel,
  discardLabel,
  saveLabel,
  note,
}: SaveBarProps) {
  if (dirtyCount === 0) return null;
  return (
    <div className="sticky bottom-3 z-20 mt-4">
      <div className="glass-surface flex flex-wrap items-center gap-x-4 gap-y-2 rounded-2xl border border-[var(--color-border)] px-4 py-2.5 shadow-lg">
        <span role="status" className="text-[13px] font-semibold">
          {unsavedLabel}
        </span>
        {note ? (
          <span className="text-[12.5px] text-[var(--color-text-muted)]">
            {note}
          </span>
        ) : null}
        <span className="ml-auto flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={onDiscard}
            disabled={saving}
            className="border border-[var(--color-border)]"
          >
            {discardLabel}
          </Button>
          <Button
            variant="cta"
            size="sm"
            onClick={onSave}
            disabled={saving}
            aria-busy={saving || undefined}
          >
            {saveLabel}
          </Button>
        </span>
      </div>
    </div>
  );
}
