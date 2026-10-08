// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// Khu vực D — one row per admin-managed problem level (admin order), cumulative (not range-filtered).
"use client";

import { useT } from "@/shared/i18n";
import { Card, ProgressBar, Skeleton, type BadgeVariant } from "@/shared/ui";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import { useDifficultyBreakdown } from "@/entities/progress";

// Seed levels keep their colours through their tone; any other level falls back to the neutral colour.
const TONE_COLOR_VAR: Partial<Record<BadgeVariant, string>> = {
  success: "--color-success",
  warn: "--color-admin-warn",
  negative: "--color-danger",
};

export function DifficultyCard() {
  const t = useT("myProgress");
  const query = useDifficultyBreakdown();
  const levels = useProblemLevels();

  return (
    <Card title={t("difficulty.title")}>
      <div className="flex flex-col gap-3.5">
        {query.isLoading || !query.data
          ? Array.from({ length: levels.length }, (_, i) => (
              <Skeleton key={i} className="h-10" />
            ))
          : levels.map((level) => {
              const row = query.data.find((r) => r.difficulty === level.key);
              const solved = row?.solvedCount ?? 0;
              const total = row?.totalCount ?? 0;
              const label = problemLevelLabel(levels, level.key);
              const color = `var(${TONE_COLOR_VAR[problemLevelTone(levels, level.key)] ?? "--color-text-muted"})`;
              return (
                <div key={level.key}>
                  <div className="mb-1.5 flex items-center gap-2">
                    <span
                      className="text-[13.5px] font-semibold"
                      style={{ color }}
                    >
                      {label}
                    </span>
                    <span className="ml-auto font-mono text-xs text-[var(--color-text-muted)]">
                      {solved} / {total}
                    </span>
                  </div>
                  <ProgressBar
                    value={solved}
                    max={Math.max(total, 1)}
                    label={label}
                    fill={color}
                    height={7}
                  />
                </div>
              );
            })}
      </div>
    </Card>
  );
}
