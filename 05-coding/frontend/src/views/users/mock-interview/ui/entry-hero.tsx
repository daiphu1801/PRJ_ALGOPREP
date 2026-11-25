// Top of the entry screen: the title on a soft brand-coloured band, with up to three figures of the learner's own
// finished sessions (real data from F5 #17 via entities/progress). With no finished session, or while the figures
// load or fail, the band shows the title alone: no placeholder numbers.
"use client";

import { Bot } from "lucide-react";
import { useInterviewSummary } from "@/entities/progress";
import { useT } from "@/shared/i18n";

function Figure({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-[7.5rem] rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] px-3.5 py-2.5">
      <p className="text-[11px] font-semibold tracking-[0.06em] text-[var(--color-text-subtle)] uppercase">
        {label}
      </p>
      <p className="mt-0.5 text-[16px] font-bold">{value}</p>
    </div>
  );
}

export function EntryHero() {
  const t = useT("mockInterview");
  // The criterion names of the progress module (codes CLARITY, ...), the same words the dashboard uses.
  const tCriteria = useT("myProgress");
  const summary = useInterviewSummary().data;
  const showFigures =
    summary !== undefined && summary.completedSessionCount > 0;
  return (
    <div className="glass-card mb-5 overflow-hidden border border-[var(--color-border)] bg-[linear-gradient(120deg,color-mix(in_srgb,var(--color-primary)_16%,transparent),transparent_65%)] p-5">
      <div className="flex flex-wrap items-center gap-4">
        <span
          aria-hidden="true"
          className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[var(--color-primary)] text-[var(--color-on-primary)]"
        >
          <Bot className="h-6 w-6" />
        </span>
        <div className="min-w-0 flex-1">
          <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("subtitle")}
          </p>
        </div>
        {showFigures ? (
          <div className="flex flex-wrap gap-2.5">
            <Figure
              label={t("hero.statSessions")}
              value={String(summary.completedSessionCount)}
            />
            {summary.averageScore === null ? null : (
              <Figure
                label={t("hero.statAverage")}
                value={`${summary.averageScore} / 10`}
              />
            )}
            {summary.weakestCriterionCode ? (
              <Figure
                label={t("hero.statWeakest")}
                value={tCriteria(
                  `interview.criteria.${summary.weakestCriterionCode}`,
                )}
              />
            ) : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}
