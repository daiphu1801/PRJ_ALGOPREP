// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, DotGrid } from "@/shared/ui";
import { useSubmissionsByDay } from "@/entities/admin-dashboard";

export function SubmissionsByDayBlock() {
  const t = useT("adminOverview");
  const query = useSubmissionsByDay();

  return (
    <DashboardBlockState
      title={t("blocks.submissionsByDay")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.weekTotal === 0 : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {query.data && (
        <div>
          <DotGrid columns={query.data.columns} accentColorVar="--color-admin-teal" />
          <p className="mt-3 flex justify-between text-xs text-[var(--color-text-muted)]">
            <span>
              {t("blocks.weekTotal")}: {query.data.weekTotal}
            </span>
            <span>
              {t("blocks.dailyAverage")}: {query.data.dailyAverage}
            </span>
          </p>
        </div>
      )}
    </DashboardBlockState>
  );
}
