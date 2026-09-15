// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, LineChartWithTotal } from "@/shared/ui";
import { useSubmissionsByMonth } from "@/entities/admin-dashboard";

export function SubmissionsByMonthBlock() {
  const t = useT("adminOverview");
  const query = useSubmissionsByMonth();

  return (
    <DashboardBlockState
      title={t("blocks.submissionsByMonth")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.monthTotal === 0 : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && (
        <LineChartWithTotal
          points={query.data.points}
          totalLabel={t("blocks.monthTotal")}
          totalValue={query.data.monthTotal.toLocaleString("vi-VN")}
          accentColorVar="--color-admin-cyan"
        />
      )}
    </DashboardBlockState>
  );
}
