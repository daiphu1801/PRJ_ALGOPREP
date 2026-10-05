// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useProblemLevels } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { DashboardBlockState, GroupedBarChart } from "@/shared/ui";
import { useDifficultyBreakdown } from "../../api";
import { buildDifficultyGroups } from "../../model/difficulty-groups";

export function DifficultyBreakdownBlock() {
  const t = useT("adminOverview");
  const query = useDifficultyBreakdown();
  const levels = useProblemLevels();
  const groups = query.data ? buildDifficultyGroups(levels, query.data) : [];

  return (
    <DashboardBlockState
      title={t("blocks.difficultyBreakdown")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? groups.every((g) => g.bars.every((b) => b.value === 0)) : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && <GroupedBarChart groups={groups} legend={query.data.legend} />}
    </DashboardBlockState>
  );
}
