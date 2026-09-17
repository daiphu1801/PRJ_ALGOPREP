// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Minus / value / plus stepper. 09-layoutBase/Admin - Cấu hình AI.dc.html:206-210 — 26px square
// buttons with an 8px radius and a 42px right-aligned monospace readout.
//
// role="spinbutton" with the real min/max/now, so a screen reader announces "30, slider, minimum 0,
// maximum 100" instead of three unrelated controls. The minus and plus signs are U+2212 and a plain
// "+", not pictographs (.claude/rules/no-emoji.md).
"use client";

import { cn } from "@/shared/lib";

export function NumberStepper({
  value,
  onValueChange,
  label,
  min = 0,
  max = 100,
  step = 5,
  format = (current: number) => String(current),
  decrementLabel,
  incrementLabel,
  disabled = false,
  className,
}: {
  value: number;
  onValueChange: (value: number) => void;
  /** Accessible name — what this number means. */
  label: string;
  min?: number;
  max?: number;
  step?: number;
  format?: (value: number) => string;
  decrementLabel: string;
  incrementLabel: string;
  disabled?: boolean;
  className?: string;
}) {
  const buttonClass =
    "flex h-[26px] w-[26px] items-center justify-center rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] text-sm leading-none disabled:opacity-40 hover:bg-[var(--color-surface-hover)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]";

  return (
    <span
      role="spinbutton"
      aria-label={label}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={value}
      aria-valuetext={format(value)}
      className={cn("flex shrink-0 items-center gap-1.5", className)}
    >
      <button
        type="button"
        className={buttonClass}
        aria-label={decrementLabel}
        disabled={disabled || value <= min}
        onClick={() => onValueChange(Math.max(min, value - step))}
      >
        −
      </button>
      <span className="min-w-[42px] text-right font-mono text-[13px] font-semibold">
        {format(value)}
      </span>
      <button
        type="button"
        className={buttonClass}
        aria-label={incrementLabel}
        disabled={disabled || value >= max}
        onClick={() => onValueChange(Math.min(max, value + step))}
      >
        +
      </button>
    </span>
  );
}
