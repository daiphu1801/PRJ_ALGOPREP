// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Minus / value / plus stepper. 09-layoutBase/Admin - Cấu hình AI.dc.html:206-210 — 26px square
// buttons with an 8px radius and a 42px right-aligned monospace readout.
//
// role="spinbutton" with the real min/max/now, so a screen reader announces "30, slider, minimum 0,
// maximum 100" instead of three unrelated controls. The minus and plus signs are U+2212 and a plain
// "+", not pictographs (.claude/rules/no-emoji.md).
//
// `editable` (off by default, so existing callers do not change) turns the readout into a text box
// the admin can type a number into (2026-10-10, ai-config and ai-limits settings). Only the number
// is in the box, drawn with a border and surface fill like a text field so it reads as typeable, and
// it is selected on focus; the unit from `format` ("lượt/giờ", "%") stays beside it as plain text,
// so typing never touches it. Enter or leaving the box commits, Escape reverts, ArrowUp and
// ArrowDown step. A value outside min and max is pulled to the nearest bound, and text that is not
// a whole number ("abc", "2.5") reverts, so the caller only ever sees a valid number.
"use client";

import { useRef, useState } from "react";
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
  editable = false,
  unitWidth,
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
  /** Lets the readout be typed into. Off keeps the plain text readout. */
  editable?: boolean;
  /**
   * Fixed width for the unit text after an editable box ("9ch"), so a column of steppers whose
   * units differ in length ("giây", "phiên/giờ") lines up their buttons (admin UX review F8).
   */
  unitWidth?: string;
  className?: string;
}) {
  // null while the box is not being edited, so it follows `value` and `format` without syncing.
  const [typed, setTyped] = useState<string | null>(null);
  // Escape blurs the box, and the blur would otherwise commit the text Escape meant to throw away.
  const cancelled = useRef(false);

  const buttonClass =
    "flex h-[26px] w-[26px] items-center justify-center rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] text-sm leading-none disabled:opacity-40 hover:bg-[var(--color-surface-hover)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]";

  function commit() {
    if (cancelled.current) {
      cancelled.current = false;
      setTyped(null);
      return;
    }
    if (typed === null) return;
    setTyped(null);
    // A whole number; a pasted unit after it ("85%") is tolerated and dropped.
    const match = /^\s*(-?\d+)\s*(?:[^\d.,-].*)?$/.exec(typed);
    if (!match) return;
    const next = Math.min(max, Math.max(min, Number(match[1])));
    if (next !== value) onValueChange(next);
  }

  // The unit is whatever `format` puts around the number: "10 lượt/giờ" gives "" before and
  // " lượt/giờ" after. If the number cannot be found in it (an odd formatter) there is no unit.
  const shown = format(value);
  const numberAt = shown.indexOf(String(value));
  const before = numberAt > 0 ? shown.slice(0, numberAt) : "";
  const after =
    numberAt >= 0 ? shown.slice(numberAt + String(value).length) : "";

  const readout = editable ? (
    <span className="flex items-center gap-1.5 font-mono text-[13px] font-semibold">
      {before ? <span>{before}</span> : null}
      <input
        type="text"
        inputMode="numeric"
        role="spinbutton"
        aria-label={label}
        aria-valuemin={min}
        aria-valuemax={max}
        aria-valuenow={value}
        aria-valuetext={format(value)}
        disabled={disabled}
        value={typed ?? String(value)}
        style={{ width: `${String(max).length + 2}ch` }}
        className="h-[26px] rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-1 text-right hover:border-[var(--color-text-subtle)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)] disabled:opacity-50"
        onFocus={(event) => event.target.select()}
        onChange={(event) => setTyped(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur();
          else if (event.key === "Escape") {
            cancelled.current = true;
            event.currentTarget.blur();
          } else if (event.key === "ArrowUp" || event.key === "ArrowDown") {
            event.preventDefault();
            const next = Math.min(
              max,
              Math.max(min, value + (event.key === "ArrowUp" ? step : -step)),
            );
            if (next !== value) onValueChange(next);
            setTyped(null);
          }
        }}
      />
      {after ? (
        <span aria-hidden="true" style={unitWidth ? { minWidth: unitWidth } : undefined}>
          {after}
        </span>
      ) : null}
    </span>
  ) : (
    <span className="min-w-[42px] text-right font-mono text-[13px] font-semibold">
      {format(value)}
    </span>
  );

  return (
    <span
      {...(editable
        ? {}
        : {
            role: "spinbutton",
            "aria-label": label,
            "aria-valuemin": min,
            "aria-valuemax": max,
            "aria-valuenow": value,
            "aria-valuetext": format(value),
          })}
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
      {readout}
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
