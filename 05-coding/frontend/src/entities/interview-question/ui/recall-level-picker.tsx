// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md.
//
// Three-button self-rating control for F6-12 (Biết rõ / Mơ hồ / Quên), reused by the quick-view
// panel and drill card of USR0401 and the Study mode of USR0402 — same three buttons, same enum,
// per BD [SoT: 02-bd/screens/users/USR0401_interview_bank_list.md Sheet 5 mục D.5].
"use client";

import { cn } from "@/shared/lib";
import { RECALL_LEVELS, type RecallLevel } from "../model/types";

export function RecallLevelPicker({
  value,
  onRate,
  labels,
  groupLabel,
  disabled = false,
  className,
}: {
  value: RecallLevel | null;
  onRate: (level: RecallLevel) => void;
  labels: Record<RecallLevel, string>;
  /** Accessible name for the group, e.g. "Mức độ nắm". */
  groupLabel: string;
  disabled?: boolean;
  className?: string;
}) {
  return (
    <div role="group" aria-label={groupLabel} className={cn("flex flex-wrap gap-1.5", className)}>
      {RECALL_LEVELS.map((level) => {
        const active = value === level;
        return (
          <button
            key={level}
            type="button"
            aria-pressed={active}
            disabled={disabled}
            onClick={() => onRate(level)}
            className={cn(
              "h-8 rounded-lg border px-3 text-[12.5px] font-semibold whitespace-nowrap transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]",
              "disabled:cursor-not-allowed disabled:opacity-50",
              active
                ? "border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-on-primary)]"
                : "border-[var(--color-border)] bg-transparent text-[var(--color-text)] hover:bg-[var(--color-surface-hover)]",
            )}
          >
            {labels[level]}
          </button>
        );
      })}
    </div>
  );
}
