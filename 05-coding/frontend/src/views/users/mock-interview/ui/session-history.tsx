// The learner's finished mock interviews, newest first, each with its date and overall score (nothing else:
// owner 2026-11-23, "chỉ cần xem điểm"). Real data from F5 #17 via entities/progress (limit 10 is the server cap).
// Rows are not links: there is no screen that reopens a finished scorecard.
"use client";

import Link from "next/link";
import { useInterviewSummary } from "@/entities/progress";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { SeeAllLink, Skeleton } from "@/shared/ui";
import { SCORE_LEVEL_COLOR, scoreLevel } from "../model/score-level";

/** The server caps the list at 10 sessions (F5 #17). */
const HISTORY_LIMIT = 10;
/** Rows in the small card on the entry screen; the history screen shows all of them. */
const PREVIEW_ROWS = 5;

function useHistory(rows: number) {
  const query = useInterviewSummary("all", HISTORY_LIMIT);
  return { query, sessions: (query.data?.recentSessions ?? []).slice(0, rows) };
}

export function SessionHistoryList({
  rows = HISTORY_LIMIT,
}: {
  rows?: number;
}) {
  const t = useT("mockInterview");
  const locale = useLocale() === "en" ? "en" : "vi";
  const { query, sessions } = useHistory(rows);
  return (
    <>
      {query.isLoading ? (
        <Skeleton className="h-24 w-full" />
      ) : query.isError ? (
        <p className="text-[13px] text-[var(--color-text-muted)]">
          {t("history.error")}
        </p>
      ) : sessions.length === 0 ? (
        <p className="text-[13px] text-[var(--color-text-muted)]">
          {t("history.empty")}
        </p>
      ) : (
        <ul className="flex flex-col divide-y divide-[var(--color-border)]">
          {sessions.map((session, index) => {
            const level =
              session.score === null ? null : scoreLevel(session.score);
            return (
              <li
                key={session.sessionId}
                className="flex items-center justify-between gap-3 py-2"
              >
                <span className="text-[13.5px]">
                  <span className="mr-2 font-mono text-[12px] text-[var(--color-text-subtle)]">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <Link
                    href={`/mock-interview/history/${session.sessionId}`}
                    className="hover:underline"
                  >
                    {formatDateVN(session.startedAt, { locale })}
                  </Link>
                </span>
                {level === null ? (
                  <span className="font-mono text-[13px]">-</span>
                ) : (
                  <span
                    className="flex items-center gap-2 font-mono text-[13px] font-bold"
                    style={{ color: SCORE_LEVEL_COLOR[level] }}
                  >
                    {session.score} / 10
                    <span className="font-sans text-[11.5px] font-semibold">
                      {t(`scoreLevel.${level}`)}
                    </span>
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}

/** Small card under the session settings: the latest few scores and a link to the full history screen. */
export function SessionHistoryCard() {
  const t = useT("mockInterview");
  const { sessions } = useHistory(PREVIEW_ROWS);
  return (
    <div className="glass-card border border-[var(--color-border)] p-5">
      <h2 className="mb-2 text-[14.5px] font-bold">{t("history.title")}</h2>
      <SessionHistoryList rows={PREVIEW_ROWS} />
      {sessions.length > 0 ? (
        <SeeAllLink href="/mock-interview/history">
          {t("history.seeAll")}
        </SeeAllLink>
      ) : null}
    </div>
  );
}
