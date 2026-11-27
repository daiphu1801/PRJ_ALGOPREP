"use client";

import { useLocale, useT } from "@/shared/i18n";
import { DashboardBlockState, LineChartWithTotal } from "@/shared/ui";
import { useSubmissionsByMonth } from "../../api";

export function SubmissionsByMonthBlock() {
  const t = useT("adminOverview");
  const locale = useLocale();
  const query = useSubmissionsByMonth();

  return (
    <DashboardBlockState
      className="@min-[880px]:flex @min-[880px]:flex-col"
      title={t("blocks.submissionsByMonth")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data?.monthTotal === 0}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      minHeightClassName="min-h-[176px] @min-[880px]:min-h-[clamp(8rem,calc(calc(30*var(--dvh1))-6rem),11rem)]"
    >
      {query.data && (
        <LineChartWithTotal
          className="@min-[880px]:flex @min-[880px]:min-h-0 @min-[880px]:flex-1 @min-[880px]:flex-col"
          points={query.data.points}
          totalLabel={t("blocks.monthTotal")}
          totalValue={query.data.monthTotal.toLocaleString(locale)}
          ariaLabel={t("blocks.submissionsByMonth")}
          accentColorVar="--color-admin-cyan"
          chartClassName="h-24 @min-[880px]:h-[clamp(2.5rem,calc(calc(32*var(--dvh1))-11.5rem),6rem)] @min-[880px]:grow"
          area
        />
      )}
    </DashboardBlockState>
  );
}
