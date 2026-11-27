"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, RankedProgressList } from "@/shared/ui";
import { useSubmissionsByLanguage } from "../../api";
import { translateLabel } from "../../model/labels";

export function SubmissionsByLanguageBlock() {
  const t = useT("adminOverview");
  const query = useSubmissionsByLanguage();
  const items = query.data?.series
    .map((series) => ({
      ...translateLabel(t, series),
      value: series.points.reduce((total, count) => total + count, 0),
    }))
    .sort((a, b) => b.value - a.value);

  return (
    <DashboardBlockState
      className="@min-[880px]:flex @min-[880px]:flex-col"
      title={t("blocks.submissionsByLanguage")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={items?.every(({ value }) => value === 0) ?? false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
    >
      {items && (
        <div className="@min-[880px]:flex @min-[880px]:flex-1 @min-[880px]:flex-col @min-[880px]:justify-center">
          <p className="mb-3 text-xs text-[var(--color-text-muted)]">
            {t("blocks.languageWindow")}
          </p>
          <RankedProgressList items={items} numbered={false} />
        </div>
      )}
    </DashboardBlockState>
  );
}
