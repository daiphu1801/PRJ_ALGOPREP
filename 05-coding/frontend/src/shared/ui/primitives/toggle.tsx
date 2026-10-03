// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// On/off switch. 9 of the 13 Admin/shared BD files describe a bật/tắt control (heaviest:
// 02-bd/screens/admin/admin_language_config.md, admin_ai_config.md, admin_permission_matrix.md).
//
// role="switch" rather than a styled checkbox: a screen reader then announces "on"/"off" instead of
// "checked", which is what these controls actually mean. `pending` exists because every Admin toggle
// writes to the server — without it each screen would invent its own in-flight treatment.
"use client";

import { useId } from "react";
import { cn } from "@/shared/lib";

type ToggleProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label: string;
  /** Renders the label visually. Off by default: most toggles sit in a row that already has a text cell. */
  showLabel?: boolean;
  disabled?: boolean;
  /** Server write in flight — blocks input and dims, without collapsing the layout. */
  pending?: boolean;
  className?: string;
};

export function Toggle({
  checked,
  onCheckedChange,
  label,
  showLabel = false,
  disabled = false,
  pending = false,
  className,
}: ToggleProps) {
  const locked = disabled || pending;
  const labelId = useId();

  const button = (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      // A <label> cannot label a <button>, so the visible-label case points at the text node by id.
      aria-label={showLabel ? undefined : label}
      aria-labelledby={showLabel ? labelId : undefined}
      aria-busy={pending || undefined}
      disabled={locked}
      onClick={() => onCheckedChange(!checked)}
      className={cn(
        "relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition-colors",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)]",
        checked ? "bg-[var(--color-admin-teal)]" : "bg-[var(--color-track)]",
        locked ? "cursor-not-allowed opacity-50" : "cursor-pointer",
        !showLabel && className,
      )}
    >
      <span
        className={cn(
          "inline-block h-3.5 w-3.5 rounded-full bg-white shadow transition-transform",
          checked ? "translate-x-[18px]" : "translate-x-[3px]",
        )}
      />
    </button>
  );

  if (!showLabel) return button;

  return (
    <span className={cn("inline-flex items-center gap-2 text-sm", className)}>
      {button}
      <span id={labelId} className={cn(locked && "opacity-50")}>
        {label}
      </span>
    </span>
  );
}
