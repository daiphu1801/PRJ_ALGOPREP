// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import type { ProgressRange } from "@/entities/progress";
import { useDashboardDaily } from "../../api";
import { useT } from "@/shared/i18n";
import { DashboardBlockState, LineChartWithTotal } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:130-157 — "Bài nộp theo ngày" with 7/30/90-day tabs and
 * a summary line reading total + per-day average.
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
  const average = points.length === 0 ? 0 : Math.round((total / points.length) * 10) / 10;

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
      minHeightClassName="min-h-[260px]"
    >
      <div>
        <LineChartWithTotal
          points={points.map((point) => ({ label: point.label, value: point.count }))}
          totalLabel={t("daily.totalLabel", { average })}
          totalValue={String(total)}
        />
      </div>
    </DashboardBlockState>
  );
}
