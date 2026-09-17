// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, HalfDonutGauge } from "@/shared/ui";
import { useVerdictDistribution } from "@/entities/admin-dashboard";

export function VerdictDistributionBlock() {
  const t = useT("adminOverview");
  const query = useVerdictDistribution();

  return (
    <DashboardBlockState
      title={t("blocks.verdictDistribution")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.slices.every((s) => s.value === 0) : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && <HalfDonutGauge slices={query.data.slices} />}
    </DashboardBlockState>
  );
}
