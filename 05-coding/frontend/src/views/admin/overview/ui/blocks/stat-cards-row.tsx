// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, StatCardWithSparkline } from "@/shared/ui";
import {
  useActiveUsersSummary,
  usePasswordResetsSummary,
  useSubmissionsSummary,
} from "../../api";

export function StatCardsRow() {
  const t = useT("adminOverview");
  const submissions = useSubmissionsSummary();
  const activeUsers = useActiveUsersSummary();
  const passwordResets = usePasswordResetsSummary();

  return (
    <div className="grid grid-cols-1 gap-3 @min-[640px]:grid-cols-3 @min-[880px]:[@media(max-height:850px)]:[&>section]:p-2 @min-[880px]:[@media(max-height:850px)]:[&>section>div]:py-2">
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
            noComparisonLabel={t("blocks.previousPeriodZero")}
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
            noComparisonLabel={t("blocks.previousPeriodZero")}
            sparklineSeries={activeUsers.data.sparklineSeries}
            accentColorVar="--color-admin-teal"
          />
        )}
      </DashboardBlockState>
      <DashboardBlockState
        title={t("blocks.passwordResetsSummary")}
        isLoading={passwordResets.isLoading}
        isError={passwordResets.isError}
        isEmpty={false}
        onRetry={() => passwordResets.refetch()}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
        minHeightClassName="min-h-[80px]"
        hideTitle
      >
        {passwordResets.data && (
          <StatCardWithSparkline
            label={t("blocks.passwordResetsSummary")}
            value={passwordResets.data.value.toLocaleString("vi-VN")}
            deltaPercent={passwordResets.data.deltaPercent}
            deltaDirection={passwordResets.data.deltaDirection}
            noComparisonLabel={t("blocks.previousPeriodZero")}
            sparklineSeries={passwordResets.data.sparklineSeries}
            accentColorVar="--color-admin-warn"
          />
        )}
      </DashboardBlockState>
    </div>
  );
}
