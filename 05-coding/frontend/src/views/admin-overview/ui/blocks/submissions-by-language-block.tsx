// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, HorizontalBarChart } from "@/shared/ui";
import { useSubmissionsByLanguage } from "@/entities/admin-dashboard";

export function SubmissionsByLanguageBlock() {
  const t = useT("adminOverview");
  const query = useSubmissionsByLanguage();

  return (
    <DashboardBlockState
      title={t("blocks.submissionsByLanguage")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.series.every((s) => s.points.every((v) => v === 0)) : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && <HorizontalBarChart series={query.data.series} pointLabels={query.data.pointLabels} />}
    </DashboardBlockState>
  );
}
