// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực E — reuses entities/progress (same GetMyProgressOverview/GetMyInterviewSummary calls
// USR0501_my_progress makes, per Sheet 6 Khu vực E note NO 6) instead of a third copy of the same
// counts.
"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Card, Skeleton } from "@/shared/ui";
import { useInterviewSummary, useProgressOverview } from "@/entities/progress";

export function PracticeCard() {
  const t = useT("profile");
  const overview = useProgressOverview();
  const interview = useInterviewSummary();

  return (
    <Card title={t("practice.title")}>
      <div className="flex flex-col gap-3 text-sm">
        <Row label={t("practice.solved")}>
          {overview.isLoading ? (
            <Skeleton className="h-5 w-10" />
          ) : (
            (overview.data?.solvedProblemCount ?? "-")
          )}
        </Row>
        <Row label={t("practice.submissions")}>
          {overview.isLoading ? (
            <Skeleton className="h-5 w-10" />
          ) : (
            (overview.data?.totalSubmissions ?? "-")
          )}
        </Row>
        <Row label={t("practice.interviews")}>
          {interview.isLoading ? (
            <Skeleton className="h-5 w-10" />
          ) : (
            (interview.data?.completedSessionCount ?? "-")
          )}
        </Row>
      </div>
      <Link
        href="/dashboard"
        className="mt-4 flex h-9 items-center justify-center rounded-lg border border-[var(--color-border)] text-sm font-semibold hover:bg-[var(--color-surface-hover)]"
      >
        {t("practice.viewFullProgress")}
      </Link>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className="h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-primary)]"
        aria-hidden="true"
      />
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className="ml-auto font-mono font-semibold text-[var(--color-text)]">
        {children}
      </span>
    </div>
  );
}
