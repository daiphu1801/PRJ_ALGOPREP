// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import {
  RUBRIC_CRITERIA,
  useInterviewSummary,
  type ProgressRange,
} from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { DashboardBlockState, ProgressBar, SeeAllLink } from "@/shared/ui";

// Tone by rubric score (0 to 10): strong / middling / weak. Tinted pill background derived from the same colour.
function scoreTone(score: number | null) {
  const color =
    score === null
      ? "var(--color-text-muted)"
      : score >= 8
        ? "var(--color-success)"
        : score >= 6
          ? "var(--color-primary)"
          : "var(--color-danger)";
  return {
    color,
    backgroundColor: `color-mix(in srgb, ${color} 12%, transparent)`,
  };
}

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:287-314 — "Mock Interview": the last 3 sessions (owner D3), each
 * with its overall rubric score and 4 per-criterion bars, plus a "Tất cả" link.
 *
 * Owner decisions 2026-10-18 (BD Q5, Q8): rubric scores use a 0 to 10 scale; the card shows one bar
 * per criterion; the screen-level `range` also filters this block (session count, average, weakest
 * item, last 3 sessions); the weakest item is one of the 4 real criteria.
 *
 * ai-review-owned (F5), so it carries its own error copy and fails alone — REQ-07 of
 * 01-rd/screens/users/USR0601_dashboard.md, and the graceful-degradation rule in CLAUDE.md.
 */
export function RecentInterviewsBlock({ range }: { range: ProgressRange }) {
  const t = useT("dashboard");
  const tCriteria = useT("myProgress");
  const query = useInterviewSummary(range);
  const summary = query.data;
  const sessions = summary?.recentSessions ?? [];

  return (
    <DashboardBlockState
      title={t("interviews.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={summary !== undefined && sessions.length === 0}
      onRetry={() => query.refetch()}
      emptyMessage={t("interviews.empty")}
      errorMessage={t("errorAi")}
      retryLabel={t("retry")}
      headingLevel={2}
      footer={<SeeAllLink href="/mock-interview">{t("seeAll")}</SeeAllLink>}
    >
      <div>
        {summary && (
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-xl bg-[var(--color-surface-hover)] px-3.5 py-2.5">
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              {summary.averageScore === null
                ? t("interviews.summaryNoScore", {
                    count: summary.completedSessionCount,
                  })
                : t("interviews.summary", {
                    count: summary.completedSessionCount,
                    score: summary.averageScore,
                  })}
            </p>
            {summary.weakestCriterionCode && (
              <span className="rounded-full bg-[color-mix(in_srgb,var(--color-danger)_12%,transparent)] px-2.5 py-0.5 text-[11.5px] font-semibold text-[var(--color-danger)]">
                {t("interviews.weakest", {
                  criterion: tCriteria(
                    `interview.criteria.${summary.weakestCriterionCode}`,
                  ),
                })}
              </span>
            )}
          </div>
        )}
        <ul className="flex flex-col gap-3">
          {sessions.map((session) => (
            // EVT-9 (open the rubric from a row) is deferred: no review route exists yet (debt "EVT-9 lui").
            <li
              key={session.sessionId}
              className="rounded-xl border border-[var(--color-border)] px-3.5 py-3"
            >
              <div className="flex items-center justify-between gap-2.5">
                <span className="truncate text-[13.5px] font-semibold">
                  {t("interviews.session", { date: formatDateVN(session.startedAt) })}
                </span>
                <span
                  className="shrink-0 rounded-full px-2.5 py-0.5 font-mono text-[12.5px] font-bold"
                  style={scoreTone(session.score)}
                >
                  {session.score === null ? "-" : `${session.score} / 10`}
                </span>
              </div>
              {session.rubricScores && (
                <ul className="mt-3 grid grid-cols-1 gap-x-5 gap-y-2.5 sm:grid-cols-2">
                  {RUBRIC_CRITERIA.map((code) => {
                    const label = tCriteria(`interview.criteria.${code}`);
                    const value = session.rubricScores![code];
                    return (
                      <li key={code}>
                        <div className="mb-1 flex items-baseline justify-between gap-2">
                          <span className="truncate text-[12px] text-[var(--color-text-muted)]">
                            {label}
                          </span>
                          <span className="shrink-0 font-mono text-[12px] font-semibold">
                            {value}
                          </span>
                        </div>
                        <ProgressBar
                          value={value}
                          max={10}
                          label={label}
                          fill={scoreTone(value).color}
                          height={6}
                        />
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          ))}
        </ul>
      </div>
    </DashboardBlockState>
  );
}
