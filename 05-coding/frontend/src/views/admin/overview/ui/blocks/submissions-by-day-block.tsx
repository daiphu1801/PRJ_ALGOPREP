// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, LineChartWithTotal } from "@/shared/ui";
import { useSubmissionsByDay } from "../../api";

export function SubmissionsByDayBlock() {
  const t = useT("adminOverview");
  const query = useSubmissionsByDay();

  return (
    <DashboardBlockState
      className="@min-[880px]:absolute @min-[880px]:inset-0 @min-[880px]:flex @min-[880px]:flex-col"
      title={t("blocks.submissionsByDay")}
      isLoading={query.isLoading}
      isError={query.isError}
      // An empty week still draws a flat line, never the empty caption (BD ADM0101 :506).
      isEmpty={false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && (
        <LineChartWithTotal
          className="@min-[880px]:flex @min-[880px]:min-h-0 @min-[880px]:flex-1 @min-[880px]:flex-col"
          points={query.data.columns.map((c) => ({
            label: c.label,
            value: c.count,
          }))}
          totalValue={query.data.weekTotal.toLocaleString("vi-VN")}
          totalLabel={`${t("blocks.weekTotal")} · ${t("blocks.dailyAverage")}: ${query.data.dailyAverage}`}
          ariaLabel={t("blocks.submissionsByDay")}
          accentColorVar="--color-admin-teal"
          chartClassName="h-[clamp(8rem,24cqw,12rem)] @min-[880px]:h-0 @min-[880px]:min-h-[clamp(3rem,calc(calc(45*var(--dvh1))-15rem),10rem)] @min-[880px]:flex-1"
          area
          smooth
        />
      )}
    </DashboardBlockState>
  );
}
