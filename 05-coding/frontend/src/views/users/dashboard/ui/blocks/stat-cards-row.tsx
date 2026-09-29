// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useInterviewSummary, useProgressOverview } from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { DashboardBlockState, StatCard } from "@/shared/ui";

/**
 * The four figures of 09-layoutBase/Dashboard AlgoPrep.dc.html:110-125 — Đã giải, Acceptance rate,
 * Streak, Mock Interview.
 *
 * Reads `entities/progress`, NOT a dashboard-owned copy: these are the same F1-06/F1-07/F1-08
 * numbers `my_progress` already shows, and a second mock of them would drift from the first the
 * moment either screen is touched. "Streak" reuses the definition settled at
 * 01-rd/screens/users/USR0501_my_progress.md mục 4 Q1 rather than defining it again here.
 *
 * Each card's region is named "Chỉ số <metric>", not the bare metric: "Mock Interview" is also the
 * heading of the recent-sessions block further down, and two landmarks with the same accessible
 * name are ambiguous to anyone navigating by region.
 *
 * TWO queries, so the AI-owned card degrades on its own: the interview summary comes from
 * `ai-review` (F5) and the other three from `identity` (F1). REQ-07 of
 * 01-rd/screens/users/USR0601_dashboard.md requires exactly that split — F5 falling over must not
 * blank the first three cards.
 */
export function StatCardsRow() {
  const t = useT("dashboard");
  const overviewQuery = useProgressOverview();
  const interviewQuery = useInterviewSummary();
  const overview = overviewQuery.data;
  const interview = interviewQuery.data;

  const acRate =
    overview && overview.totalSubmissions > 0
      ? Math.round((overview.acceptedCount / overview.totalSubmissions) * 100)
      : null;

  return (
    <div className="mb-3.5 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(190px,1fr))]">
      <DashboardBlockState
        title={t("stats.regionLabel", { metric: t("stats.solved") })}
        hideTitle
        minHeightClassName="min-h-[104px]"
        isLoading={overviewQuery.isLoading}
        isError={overviewQuery.isError}
        isEmpty={false}
        onRetry={() => overviewQuery.refetch()}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
      >
        {overview && (
          <StatCard
            label={t("stats.solved")}
            value={overview.solvedProblemCount}
            delta={`/ ${overview.totalPublishedProblemCount}`}
            meta={t("stats.solvedMeta", { total: overview.totalPublishedProblemCount })}
            className="border-0 p-0"
          />
        )}
      </DashboardBlockState>

      <DashboardBlockState
        title={t("stats.regionLabel", { metric: t("stats.acRate") })}
        hideTitle
        minHeightClassName="min-h-[104px]"
        isLoading={overviewQuery.isLoading}
        isError={overviewQuery.isError}
        isEmpty={acRate === null}
        onRetry={() => overviewQuery.refetch()}
        emptyMessage={t("stats.acRateEmpty")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
      >
        {overview && acRate !== null && (
          <StatCard
            label={t("stats.acRate")}
            value={`${acRate}%`}
            meta={t("stats.acRateMeta", {
              submissions: overview.totalSubmissions,
              accepted: overview.acceptedCount,
            })}
            className="border-0 p-0"
          />
        )}
      </DashboardBlockState>

      <DashboardBlockState
        title={t("stats.regionLabel", { metric: t("stats.streak") })}
        hideTitle
        minHeightClassName="min-h-[104px]"
        isLoading={overviewQuery.isLoading}
        isError={overviewQuery.isError}
        isEmpty={false}
        onRetry={() => overviewQuery.refetch()}
        emptyMessage={t("emptyGeneric")}
        errorMessage={t("errorGeneric")}
        retryLabel={t("retry")}
      >
        {overview && (
          <StatCard
            label={t("stats.streak")}
            value={overview.currentStreakDays}
            delta={t("stats.streakUnit")}
            meta={t("stats.streakMeta", { avg: overview.avgAttemptsPerSolved ?? 0 })}
            className="border-0 p-0"
          />
        )}
      </DashboardBlockState>

      <DashboardBlockState
        title={t("stats.regionLabel", { metric: t("stats.interview") })}
        hideTitle
        minHeightClassName="min-h-[104px]"
        isLoading={interviewQuery.isLoading}
        isError={interviewQuery.isError}
        isEmpty={interview ? interview.completedSessionCount === 0 : false}
        onRetry={() => interviewQuery.refetch()}
        emptyMessage={t("stats.interviewEmpty")}
        errorMessage={t("errorAi")}
        retryLabel={t("retry")}
      >
        {interview && (
          <StatCard
            label={t("stats.interview")}
            value={interview.completedSessionCount}
            delta={t("stats.interviewUnit")}
            meta={
              interview.averageScore === null
                ? t("stats.interviewNoScore")
                : t("stats.interviewMeta", { score: interview.averageScore })
            }
            className="border-0 p-0"
          />
        )}
      </DashboardBlockState>
    </div>
  );
}
