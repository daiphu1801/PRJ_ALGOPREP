// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực A. `acRate`/`avgAttemptsPerSolved` hide behind "-" instead of a fabricated 0%/0.0 when
// the denominator is 0 (Sheet 6 Khu vực A NO 3/5). Interview count/score read from
// `useInterviewSummary` (ai-review) and degrade to "-" independently of the rest of the bar.
"use client";

import { useT } from "@/shared/i18n";
import { SegmentedTabs, Skeleton } from "@/shared/ui";
import { useInterviewSummary, useProgressOverview, type ProgressRange } from "@/entities/progress";

export function StatsBar({ range, onRangeChange }: { range: ProgressRange; onRangeChange: (range: ProgressRange) => void }) {
  const t = useT("myProgress");
  const overview = useProgressOverview();
  const interview = useInterviewSummary();

  const acRate =
    overview.data && overview.data.totalSubmissions > 0
      ? Math.round((overview.data.acceptedCount / overview.data.totalSubmissions) * 100)
      : null;

  return (
    <div className="mb-4 flex flex-wrap items-center gap-5">
      <Stat label={t("stats.solved")} value={overview.data ? `${overview.data.solvedProblemCount} / ${overview.data.totalPublishedProblemCount}` : undefined} loading={overview.isLoading} />
      <Stat
        label={t("stats.submissions")}
        value={overview.data ? String(overview.data.totalSubmissions) : undefined}
        unit={acRate !== null ? `${acRate}% AC` : undefined}
        loading={overview.isLoading}
      />
      <Stat label={t("stats.streak")} value={overview.data ? `${overview.data.currentStreakDays}` : undefined} unit={t("stats.days")} loading={overview.isLoading} />
      <Stat
        label={t("stats.avgAttempts")}
        value={overview.data?.avgAttemptsPerSolved != null ? overview.data.avgAttemptsPerSolved.toFixed(1) : "-"}
        loading={overview.isLoading}
      />
      <Stat
        label={t("stats.interviews")}
        value={interview.data ? String(interview.data.completedSessionCount) : "-"}
        unit={interview.data?.averageScore != null ? `${interview.data.averageScore} / 5` : undefined}
        loading={interview.isLoading}
      />

      <div className="ml-auto">
        <SegmentedTabs
          label={t("stats.rangeTabs")}
          value={range}
          onValueChange={onRangeChange}
          options={[
            { value: "7d", label: t("stats.range7d") },
            { value: "30d", label: t("stats.range30d") },
            { value: "all", label: t("stats.rangeAll") },
          ]}
        />
      </div>
    </div>
  );
}

function Stat({ label, value, unit, loading }: { label: string; value: string | undefined; unit?: string; loading: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">{label}</span>
      {loading ? (
        <Skeleton className="h-4 w-10" />
      ) : (
        <>
          <span className="font-mono text-sm font-semibold">{value}</span>
          {unit ? <span className="text-xs text-[var(--color-text-muted)]">{unit}</span> : null}
        </>
      )}
    </div>
  );
}
