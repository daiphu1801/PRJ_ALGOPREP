// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, GroupedBarChart } from "@/shared/ui";
import { useDifficultyBreakdown } from "@/entities/admin-dashboard";

export function DifficultyBreakdownBlock() {
  const t = useT("adminOverview");
  const query = useDifficultyBreakdown();

  return (
    <DashboardBlockState
      title={t("blocks.difficultyBreakdown")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.groups.every((g) => g.bars.every((b) => b.value === 0)) : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && <GroupedBarChart groups={query.data.groups} legend={query.data.legend} />}
    </DashboardBlockState>
  );
}
