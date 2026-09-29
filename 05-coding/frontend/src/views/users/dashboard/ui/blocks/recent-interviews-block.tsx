// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import Link from "next/link";
import { useInterviewSummary } from "@/entities/progress";
import { useT } from "@/shared/i18n";
import { DashboardBlockState } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:287-314 — "Mock Interview": the three most recent
 * sessions with their rubric score, plus a "Tất cả" link.
 *
 * ai-review-owned (F5), so it carries its own error copy and fails alone — REQ-07 of
 * 01-rd/screens/users/USR0601_dashboard.md, and the graceful-degradation rule in CLAUDE.md.
 *
 * The prototype draws 4 per-criterion rubric bars per card. `InterviewSummary.recentSessions`
 * carries only the overall score, so those bars are NOT faked here — a per-criterion breakdown per
 * session needs a field 02-bd/screens/users/USR0501_my_progress.md Sheet 7.1 does not define.
 * Logged in 06-plan/PROTOTYPE_DEBT.md section 11.
 */
export function RecentInterviewsBlock() {
  const t = useT("dashboard");
  const query = useInterviewSummary();
  const sessions = query.data?.recentSessions ?? [];

  return (
    <DashboardBlockState
      title={t("interviews.title")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data !== undefined && sessions.length === 0}
      onRetry={() => query.refetch()}
      emptyMessage={t("interviews.empty")}
      errorMessage={t("errorAi")}
      retryLabel={t("retry")}
    >
      <div>
        <ul className="flex flex-col gap-2.5">
          {sessions.map((session) => (
            <li
              key={session.sessionId}
              className="rounded-lg border border-[var(--color-border)] px-3 py-2.5"
            >
              <div className="flex items-center justify-between gap-2.5">
                <span className="truncate text-[13.5px] font-semibold">
                  {t("interviews.session", { date: session.startedAt })}
                </span>
                <span className="shrink-0 font-mono text-[13px] font-bold">
                  {session.score === null ? "-" : `${session.score} / 5`}
                </span>
              </div>
            </li>
          ))}
        </ul>
        <Link href="/submissions" className="mt-2.5 inline-block text-[12.5px] font-semibold underline">
          {t("seeAll")}
        </Link>
      </div>
    </DashboardBlockState>
  );
}
