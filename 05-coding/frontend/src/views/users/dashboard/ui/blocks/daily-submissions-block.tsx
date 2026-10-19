// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import type { ProgressRange } from "@/entities/progress";
import { useDashboardDaily } from "../../api";
import { useT } from "@/shared/i18n";
import { DashboardBlockState, LineChartWithTotal } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:130-157 — "Bài nộp theo ngày" with a summary line reading
 * total + per-day average. Bars: 7 for "7d", 14 for "30d", one per week for "all" (owner, 2026-10-18).
 *
 * The range arrives as a PROP, not local state. It used to be local (REQ-05 said the tabs
 * re-compute this block only), but after `my_progress` merged into this screen there is one
 * screen-wide range control on the greeting row — two controls with different vocabularies on one
 * page would have been worse than the requirement the original wording was protecting.
 */
export function DailySubmissionsBlock({ range }: { range: ProgressRange }) {
  const t = useT("dashboard");
  const query = useDashboardDaily(range);

  const points = query.data ?? [];
  const total = points.reduce((sum, point) => sum + point.count, 0);
  // D6: divide by the real number of days in the range, not by the column count (a column is 2 days
  // for "30d" and a week for "all").
  const days = range === "7d" ? 7 : range === "30d" ? 30 : points.length * 7;
  const average = days === 0 ? 0 : Math.round((total / days) * 10) / 10;

  return (
    <DashboardBlockState
      title={t("daily.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={points.length > 0 && total === 0}
      onRetry={() => query.refetch()}
      emptyMessage={t("daily.empty")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      headingLevel={2}
      minHeightClassName="min-h-[260px]"
    >
      <div>
        <LineChartWithTotal
          points={points.map((point) => ({
            label: point.label,
            value: point.count,
          }))}
          totalLabel={t("daily.totalLabel", { average })}
          totalValue={String(total)}
          chartClassName="h-40"
          ariaLabel={t("daily.chartLabel", { total, average })}
        />
      </div>
    </DashboardBlockState>
  );
}
