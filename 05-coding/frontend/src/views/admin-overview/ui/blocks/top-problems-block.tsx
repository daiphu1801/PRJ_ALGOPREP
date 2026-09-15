// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, RankedProgressList } from "@/shared/ui";
import { useTopProblems } from "@/entities/admin-dashboard";

// Only block on this screen with a link onward — to problem_management
// (02-bd/screens/admin/admin_overview.md section 2 point 5, DEC-2026-0831-admin-overview-ui-decisions).
export function TopProblemsBlock() {
  const t = useT("adminOverview");
  const query = useTopProblems();

  return (
    <DashboardBlockState
      title={t("blocks.topProblems")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.items.length === 0 : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && (
        <RankedProgressList
          items={query.data.items}
          viewAllHref="/admin/problems"
          viewAllLabel={t("blocks.viewAll")}
          accentColorVar="--color-admin-cyan"
        />
      )}
    </DashboardBlockState>
  );
}
