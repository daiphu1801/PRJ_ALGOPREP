// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useActivityCalendar } from "../../api";
import { useT } from "@/shared/i18n";
import { ActivityHeatmap, DashboardBlockState } from "@/shared/ui";

/** 09-layoutBase/Dashboard AlgoPrep.dc.html:187-213 — "Hoạt động 12 tháng", 52x7 grid + legend. */
export function ActivityBlock() {
  const t = useT("dashboard");
  const query = useActivityCalendar();
  const calendar = query.data;

  return (
    <DashboardBlockState
      title={t("activity.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={calendar ? calendar.activeDayCount === 0 : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("activity.empty")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      minHeightClassName="min-h-[180px]"
    >
      {calendar && (
        <div>
          <p className="mb-2 text-xs text-[var(--color-text-muted)]">
            {t("activity.summary", { days: calendar.activeDayCount })}
          </p>
          <ActivityHeatmap
            weeks={calendar.weeks.map((week) =>
              week.map((day) => ({
                key: day.day,
                level: day.level,
                title:
                  day.count === 0
                    ? t("activity.tooltipNone", { day: day.day })
                    : t("activity.tooltip", { day: day.day, count: day.count }),
              })),
            )}
            monthLabels={calendar.monthLabels}
            lessLabel={t("activity.less")}
            moreLabel={t("activity.more")}
            ariaLabel={t("activity.title")}
          />
        </div>
      )}
    </DashboardBlockState>
  );
}
