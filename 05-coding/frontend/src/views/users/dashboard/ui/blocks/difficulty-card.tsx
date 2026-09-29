// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// Khu vực D — always 3 rows, cumulative (not range-filtered), per Sheet 6 Khu vực D.
"use client";

import { useT } from "@/shared/i18n";
import { Card, ProgressBar, Skeleton } from "@/shared/ui";
import { useDifficultyBreakdown } from "@/entities/progress";

const COLOR_VAR: Record<string, string> = { EASY: "--color-success", MEDIUM: "--color-admin-warn", HARD: "--color-danger" };

export function DifficultyCard() {
  const t = useT("myProgress");
  const query = useDifficultyBreakdown();

  return (
    <Card title={t("difficulty.title")}>
      <div className="flex flex-col gap-3.5">
        {query.isLoading || !query.data
          ? Array.from({ length: 3 }, (_, i) => <Skeleton key={i} className="h-10" />)
          : query.data.map((row) => (
              <div key={row.difficulty}>
                <div className="mb-1.5 flex items-center gap-2">
                  <span className="text-[13.5px] font-semibold" style={{ color: `var(${COLOR_VAR[row.difficulty]})` }}>
                    {t(`difficulty.${row.difficulty}`)}
                  </span>
                  <span className="ml-auto font-mono text-xs text-[var(--color-text-muted)]">
                    {row.solvedCount} / {row.totalCount}
                  </span>
                </div>
                <ProgressBar
                  value={row.solvedCount}
                  max={Math.max(row.totalCount, 1)}
                  label={t(`difficulty.${row.difficulty}`)}
                  fill={`var(${COLOR_VAR[row.difficulty]})`}
                  height={7}
                />
              </div>
            ))}
      </div>
    </Card>
  );
}
