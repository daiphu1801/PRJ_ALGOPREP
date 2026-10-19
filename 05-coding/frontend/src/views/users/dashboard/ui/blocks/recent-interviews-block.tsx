// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import Link from "next/link";
import {
  RUBRIC_CRITERIA,
  useInterviewSummary,
  type ProgressRange,
} from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { DashboardBlockState, ProgressBar } from "@/shared/ui";

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
      footer={
        <Link
          href="/mock-interview"
          className="mt-2.5 inline-block text-[12.5px] font-semibold underline"
        >
          {t("seeAll")}
        </Link>
      }
    >
      <div>
        {summary && (
          <p className="mb-2.5 text-[12.5px] text-[var(--color-text-muted)]">
            {summary.averageScore === null
              ? t("interviews.summaryNoScore", {
                  count: summary.completedSessionCount,
                })
              : t("interviews.summary", {
                  count: summary.completedSessionCount,
                  score: summary.averageScore,
                })}
            {summary.weakestCriterionCode &&
              ` · ${t("interviews.weakest", {
                criterion: tCriteria(
                  `interview.criteria.${summary.weakestCriterionCode}`,
                ),
              })}`}
          </p>
        )}
        <ul className="flex flex-col gap-2.5">
          {sessions.map((session) => (
            // EVT-9 (open the rubric from a row) is deferred: no review route exists yet (debt "EVT-9 lui").
            <li
              key={session.sessionId}
              className="rounded-lg border border-[var(--color-border)] px-3 py-2.5"
            >
              <div className="flex items-center justify-between gap-2.5">
                <span className="truncate text-[13.5px] font-semibold">
                  {t("interviews.session", { date: formatDateVN(session.startedAt) })}
                </span>
                <span className="shrink-0 font-mono text-[13px] font-bold">
                  {session.score === null ? "-" : `${session.score} / 10`}
                </span>
              </div>
              {session.rubricScores && (
                <ul className="mt-2 flex flex-col gap-1.5">
                  {RUBRIC_CRITERIA.map((code) => {
                    const label = tCriteria(`interview.criteria.${code}`);
                    const value = session.rubricScores![code];
                    return (
                      <li key={code} className="flex items-center gap-2">
                        <span className="w-36 shrink-0 truncate text-[12px] text-[var(--color-text-muted)]">
                          {label}
                        </span>
                        <div className="min-w-0 flex-1">
                          <ProgressBar
                            value={value}
                            max={10}
                            label={label}
                            fill="var(--color-primary)"
                            height={6}
                          />
                        </div>
                        <span className="w-7 shrink-0 text-right font-mono text-[12px]">
                          {value}
                        </span>
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
