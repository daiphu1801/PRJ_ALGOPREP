// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Segmented control: a sunken track with one raised pill marking the active option.
// 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:182-189 (category filter) — 3px track padding,
// 12px track radius, 28px buttons with a 9px radius. The same shape recurs as the theme switch in
// the sidebar, so it is a screen-level filter control here, not a one-off.
//
// role="tablist" is deliberately NOT used: these buttons filter a list in place, they do not swap
// between tab panels, and announcing them as tabs would promise a panel relationship that does not
// exist. `aria-pressed` on plain buttons says what is actually happening.
"use client";

import { cn } from "@/shared/lib";

type SegmentedTabsProps<T extends string> = {
  /** Accessible name for the group, e.g. "Lọc theo phân loại". */
  label: string;
  options: { value: T; label: string }[];
  value: T;
  onValueChange: (value: T) => void;
  className?: string;
};

export function SegmentedTabs<T extends string>({
  label,
  options,
  value,
  onValueChange,
  className,
}: SegmentedTabsProps<T>) {
  return (
    <div
      role="group"
      aria-label={label}
      className={cn(
        "flex gap-0.5 rounded-xl border border-[var(--color-border)] bg-[var(--color-track)] p-[3px]",
        className,
      )}
    >
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={active}
            onClick={() => onValueChange(option.value)}
            className={cn(
              "h-7 rounded-[9px] px-3 text-[12.5px] font-semibold transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]",
              active
                ? "bg-[var(--color-primary)] text-[var(--color-on-primary)] shadow-sm"
                : "text-[var(--color-text-muted)] hover:text-[var(--color-text)]",
            )}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
