// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useTopicProgress } from "@/entities/progress";
import { deriveSkillRadar } from "../../model/derive-skill-radar";
import { useT } from "@/shared/i18n";
import { DashboardBlockState, RadarChart } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:159-185 — "Năng lực theo chủ đề", 6-axis radar on a
 * 0-100 scale with the subtitle spelling out where the number comes from.
 *
 * No endpoint and no mock of its own: the score is derived client-side from the same
 * `entities/progress` topic rows the table on `my_progress` shows
 * (01-rd/screens/users/USR0601_dashboard.md mục 4 Q2 — derived display, never an AI call). Same
 * derivation feeds `GreetingBlock`, so the two always name the same weakest topic.
 */
export function SkillRadarBlock() {
  const t = useT("dashboard");
  const query = useTopicProgress("all");
  const points = query.data ? deriveSkillRadar(query.data) : [];

  return (
    <DashboardBlockState
      title={t("radar.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      // The chart needs at least 3 axes to be a polygon; fewer topics means there is nothing to plot.
      isEmpty={query.data !== undefined && points.length < 3}
      onRetry={() => query.refetch()}
      emptyMessage={t("radar.empty")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      minHeightClassName="min-h-[260px]"
    >
      <div>
        <p className="mb-1 text-xs text-[var(--color-text-muted)]">
          {t("radar.subtitle")}
        </p>
        <div className="flex justify-center">
          <RadarChart
            points={points.map((p) => ({ label: p.topicName, value: p.score }))}
            ariaLabel={t("radar.title")}
          />
        </div>
      </div>
    </DashboardBlockState>
  );
}
