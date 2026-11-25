// One finished session in detail (USR0302 history detail): the scorecard of a COMPLETED session on top, the
// conversation turn by turn below. An ABANDONED session has no scorecard, so only the conversation is shown.
// Read-only; data from F5 section 9 (the same read the running screen uses for a reload).
"use client";

import { ArrowLeft } from "lucide-react";
import { useInterviewSessionDetail } from "@/entities/mock-interview";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN, type DateLocale } from "@/shared/lib";
import {
  Badge,
  EmptyState,
  IconAction,
  QueryError,
  Skeleton,
} from "@/shared/ui";
import { InterviewResultScreen } from "./interview-result";

export function MockInterviewSessionDetailView({
  sessionId,
}: {
  sessionId: string;
}) {
  const t = useT("mockInterview");
  const locale = useLocale() as DateLocale;
  const query = useInterviewSessionDetail(sessionId);
  const detail = query.data;

  const back = (
    <IconAction
      icon={ArrowLeft}
      label={t("history.detail.back")}
      href="/mock-interview/history"
    />
  );

  if (query.isPending) {
    return (
      <section className="p-6">
        <Skeleton className="h-64 w-full" />
      </section>
    );
  }
  if (query.isError || !detail) {
    return (
      <section className="p-6">
        <div className="mb-3">{back}</div>
        <QueryError query={query} />
      </section>
    );
  }

  const { session, turns, result } = detail;
  const completed = session.stage === "completed";
  return (
    <section className="p-6">
      <div className="mb-4 flex items-start gap-3">
        {back}
        <div>
          <h1 className="text-lg font-bold tracking-tight">
            {t("history.detail.title")}
          </h1>
          <p className="flex flex-wrap items-center gap-2 text-[12.5px] text-[var(--color-text-muted)]">
            {formatDateVN(session.startedAt, { locale, withTime: true })}
            <Badge variant={completed ? "success" : "neutral"}>
              {t(
                completed
                  ? "history.status.completed"
                  : "history.status.abandoned",
              )}
            </Badge>
          </p>
        </div>
      </div>

      {result ? (
        <InterviewResultScreen result={result} />
      ) : (
        <p className="mb-4 rounded-xl border border-[var(--color-border)] px-4 py-3 text-[13px] text-[var(--color-text-muted)]">
          {t("history.detail.noScore")}
        </p>
      )}

      <div className="glass-card border border-[var(--color-border)] p-5">
        <h2 className="mb-3 text-[14.5px] font-bold">
          {t("history.detail.transcript")}
        </h2>
        {turns.length === 0 ? (
          <EmptyState>{t("history.detail.noTurns")}</EmptyState>
        ) : (
          <ol className="flex flex-col gap-3">
            {turns.map((turn) => (
              <li
                key={turn.turnIndex}
                className={
                  "flex flex-col " +
                  (turn.role === "user" ? "items-end" : "items-start")
                }
              >
                <p className="mb-1 text-[11.5px] font-semibold text-[var(--color-text-subtle)]">
                  {turn.role === "user" ? t("youName") : t("aiName")} ·{" "}
                  {t(`stage.${turn.stage}`)}
                </p>
                <p
                  className={
                    "max-w-[78%] rounded-2xl px-3.5 py-2.5 text-[13.5px] leading-relaxed whitespace-pre-line " +
                    (turn.role === "user"
                      ? "rounded-br-md bg-[var(--color-primary)] text-[var(--color-on-primary)]"
                      : "rounded-bl-md bg-[var(--color-surface-hover)]")
                  }
                >
                  {turn.content}
                </p>
              </li>
            ))}
          </ol>
        )}
      </div>
    </section>
  );
}
