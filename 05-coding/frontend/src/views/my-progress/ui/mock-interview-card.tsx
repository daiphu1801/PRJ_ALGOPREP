// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực F. "Bắt đầu một phiên mới" links to /interview-bank — USR0302_mock_interview itself is
// not mounted as a student route yet, and RD names the question bank as one of F5-24's two
// self-practice entry points [SoT: 01-rd/screens/users/USR0503_settings.md:40] — [SoT: Suy luận]
// for which of the two, tracked in the ledger row.
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Card, EmptyState, Skeleton } from "@/shared/ui";
import { useInterviewSummary } from "@/entities/progress";

export function MockInterviewCard() {
  const t = useT("myProgress");
  const query = useInterviewSummary();

  return (
    <Card
      title={t("interview.title")}
      action={
        <Link href="/interview-bank" className="text-xs font-semibold text-[var(--color-primary)] hover:underline">
          {t("interview.newSession")}
        </Link>
      }
    >
      {query.isLoading ? (
        <Skeleton className="h-24" />
      ) : query.isError || !query.data ? (
        <EmptyState>{t("interview.degradedNotice")}</EmptyState>
      ) : (
        <div className="flex flex-col gap-1">
          <Row label={t("interview.sessionCount")} value={String(query.data.completedSessionCount)} />
          <Row label={t("interview.avgScore")} value={query.data.averageScore != null ? `${query.data.averageScore} / 5` : "-"} />
          <Row
            label={t("interview.weakest")}
            value={query.data.weakestCriterionCode ? t(`interview.criteria.${query.data.weakestCriterionCode}`) : "-"}
            tone="warn"
          />
        </div>
      )}
    </Card>
  );
}

function Row({ label, value, tone }: { label: string; value: string; tone?: "warn" }) {
  return (
    <div className="flex items-center gap-3 border-b border-[var(--color-border)] py-2 text-sm last:border-0">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span
        className="ml-auto font-mono font-semibold"
        style={tone === "warn" ? { color: "var(--color-admin-warn)" } : undefined}
      >
        {value}
      </span>
    </div>
  );
}
