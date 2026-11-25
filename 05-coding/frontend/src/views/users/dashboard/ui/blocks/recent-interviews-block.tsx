// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import {
  RUBRIC_CRITERIA,
  useInterviewSummary,
  type ProgressRange,
} from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { DashboardBlockState, SeeAllLink } from "@/shared/ui";

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
 * on one line with its overall rubric score and its weakest criterion, plus a "Tất cả" link
 * (DEC-2026-1123-student-dashboard-trim replaced the 4 bars per session and the summary strip).
 *
 * Owner decisions 2026-10-18 (BD Q5, Q8): rubric scores use a 0 to 10 scale; the card shows one bar
 * per criterion (superseded 2026-11-23: one line, weakest criterion only); the screen-level `range` also filters this block (session count, average, weakest
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
      {/* One line per session (owner 2026-11-23): the count and average already sit on the stat card above, so
          the summary strip is gone, and the four bars per session became the one criterion to work on. */}
      <ul className="flex flex-col divide-y divide-[var(--color-border)]">
        {sessions.map((session) => {
          const weakest = session.rubricScores
            ? RUBRIC_CRITERIA.reduce((low, code) =>
                session.rubricScores![code] < session.rubricScores![low]
                  ? code
                  : low,
              )
            : null;
          return (
            // EVT-9 (open the rubric from a row) is deferred: no review route exists yet (debt "EVT-9 lui").
            <li
              key={session.sessionId}
              className="flex items-center justify-between gap-3 py-2"
            >
              <span className="min-w-0">
                <span className="block truncate text-[13.5px] font-semibold">
                  {t("interviews.session", {
                    date: formatDateVN(session.startedAt),
                  })}
                </span>
                {weakest ? (
                  <span className="block truncate text-[12px] text-[var(--color-text-muted)]">
                    {t("interviews.sessionWeakest", {
                      criterion: tCriteria(`interview.criteria.${weakest}`),
                      score: session.rubricScores![weakest],
                    })}
                  </span>
                ) : null}
              </span>
              <span
                className="shrink-0 rounded-full px-2.5 py-0.5 font-mono text-[12.5px] font-bold"
                style={scoreTone(session.score)}
              >
                {session.score === null ? "-" : `${session.score} / 10`}
              </span>
            </li>
          );
        })}
      </ul>
    </DashboardBlockState>
  );
}
