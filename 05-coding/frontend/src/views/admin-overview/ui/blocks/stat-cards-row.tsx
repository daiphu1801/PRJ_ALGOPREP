// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, StatCardWithSparkline } from "@/shared/ui";
import { useActiveUsersSummary, useSubmissionsSummary } from "@/entities/admin-dashboard";

// Row 1, left column: two vertical stat cards (02-bd/screens/admin/admin_overview.md section 2 point 3).
export function StatCardsRow() {
  const t = useT("adminOverview");
  const submissions = useSubmissionsSummary();
  const activeUsers = useActiveUsersSummary();

  return (
    <div className="flex flex-col gap-3">
      <DashboardBlockState
        title={t("blocks.submissionsSummary")}
        isLoading={submissions.isLoading}
        isError={submissions.isError}
        isEmpty={false}
        onRetry={() => submissions.refetch()}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
        minHeightClassName="min-h-[80px]"
        hideTitle
      >
        {submissions.data && (
          <StatCardWithSparkline
            label={t("blocks.submissionsSummary")}
            value={submissions.data.value.toLocaleString("vi-VN")}
            deltaPercent={submissions.data.deltaPercent}
            deltaDirection={submissions.data.deltaDirection}
            sparklineSeries={submissions.data.sparklineSeries}
            accentColorVar="--color-admin-cyan"
          />
        )}
      </DashboardBlockState>
      <DashboardBlockState
        title={t("blocks.activeUsersSummary")}
        isLoading={activeUsers.isLoading}
        isError={activeUsers.isError}
        isEmpty={false}
        onRetry={() => activeUsers.refetch()}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
        minHeightClassName="min-h-[80px]"
        hideTitle
      >
        {activeUsers.data && (
          <StatCardWithSparkline
            label={t("blocks.activeUsersSummary")}
            value={activeUsers.data.value.toLocaleString("vi-VN")}
            deltaPercent={activeUsers.data.deltaPercent}
            deltaDirection={activeUsers.data.deltaDirection}
            sparklineSeries={activeUsers.data.sparklineSeries}
            accentColorVar="--color-admin-teal"
          />
        )}
      </DashboardBlockState>
    </div>
  );
}
