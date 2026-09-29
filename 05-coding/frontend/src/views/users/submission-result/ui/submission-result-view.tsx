// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0201_submission_result).
//
// Layout follows 02-bd/screens/users/USR0201_submission_result.md Sheet 4.4, six areas top to
// bottom: breadcrumb (A), verdict banner (B), run stats (C), testcase table (D), code viewer (E),
// next actions (F). Reference mockup: 09-layoutBase/Kết quả nộp bài.dc.html.
//
// Key BD rule carried over exactly (Sheet 6, Khu vực D NO 1): the ENTIRE testcase table is hidden
// when status === "COMPILE_ERROR" — no testcase ever ran, so there is nothing to list. The compile
// error box (Khu vực E NO 4) takes its place instead.
//
// No real STOMP socket here — the mock has no server to push from. The "realtime" model
// (02-bd/screens/users/USR0201_submission_result.md Sheet 8 EVT-9) is out of scope for this
// prototype; the fixture for a JUDGING submission just renders a fixed partial state.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { fetchSubmissionDetail, type SubmissionTestcaseResult } from "@/entities/submission";
import { useT } from "@/shared/i18n";
import { Badge, Button, EmptyState, type BadgeVariant } from "@/shared/ui";

const VERDICT_VARIANT: Record<string, BadgeVariant> = {
  ACCEPTED: "success",
  WRONG_ANSWER: "negative",
  TIME_LIMIT_EXCEEDED: "warn",
  MEMORY_LIMIT_EXCEEDED: "warn",
  RUNTIME_ERROR: "negative",
  COMPILE_ERROR: "warn",
  PENDING: "neutral",
  COMPILING: "neutral",
  JUDGING: "cyan",
};

const TESTCASE_VERDICT_VARIANT: Record<string, BadgeVariant> = {
  AC: "success",
  WA: "negative",
  TLE: "warn",
  MLE: "warn",
  RE: "negative",
};

function formatMs(ms: number | null): string {
  return ms == null ? "-" : `${ms} ms`;
}

function formatMb(kb: number | null): string {
  return kb == null ? "-" : `${(kb / 1024).toFixed(1)} MB`;
}

export function SubmissionResultView() {
  const t = useT("submissionResult");
  const params = useParams<{ submissionId?: string }>();
  const submissionId = params?.submissionId ?? "";
  const submission = useMemo(() => fetchSubmissionDetail(submissionId), [submissionId]);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  if (!submission) {
    return <EmptyState>{t("notFound")}</EmptyState>;
  }

  const isCompileError = submission.status === "COMPILE_ERROR";
  const canOpenAi = submission.status === "ACCEPTED";

  function copyCode() {
    void navigator.clipboard?.writeText(submission!.sourceCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="p-6">
      {/* Khu vực A — Breadcrumb */}
      <nav aria-label={t("breadcrumbLabel")} className="mb-4 flex flex-wrap items-center gap-1.5 text-[12.5px] text-[var(--color-text-muted)]">
        <Link href="/problems" className="hover:text-[var(--color-text)]">
          {t("breadcrumbProblems")}
        </Link>
        <span aria-hidden="true">/</span>
        <Link href={`/problems/${submission.problemSlug}`} className="hover:text-[var(--color-text)]">
          {submission.problemTitle}
        </Link>
        <span aria-hidden="true">/</span>
        <span>{t("breadcrumbCurrent", { id: submission.id })}</span>
      </nav>

      {/* Khu vực B — Verdict banner */}
      <div className="glass-card mb-4 border border-[var(--color-border)] px-5 py-4">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant={VERDICT_VARIANT[submission.status] ?? "neutral"}>
            {t(`verdict.${submission.status}`)}
          </Badge>
          <Badge variant="neutral">{t(`difficulty.${submission.difficulty}`)}</Badge>
          {submission.totalCount != null ? (
            <span className="font-mono text-sm font-semibold">
              {submission.passedCount}/{submission.totalCount}
            </span>
          ) : null}
        </div>
        <p className="mt-1.5 text-[12.5px] text-[var(--color-text-muted)]">
          {t("verdictSubtitle", {
            passed: submission.passedCount ?? 0,
            total: submission.totalCount ?? 0,
            submittedAt: new Date(submission.submittedAt).toLocaleString("vi-VN"),
          })}
        </p>
      </div>

      {/* Khu vực C — Run stats, 6 cards */}
      <div className="mb-4 grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(140px,1fr))]">
        <StatTile label={t("stats.testcase")} value={submission.totalCount != null ? `${submission.passedCount}/${submission.totalCount}` : "-"} />
        <StatTile label={t("stats.runtime")} value={formatMs(submission.runtimeMs)} />
        <StatTile label={t("stats.memory")} value={formatMb(submission.memoryKb)} />
        <StatTile
          label={t("stats.beats")}
          value={submission.beatsPercent != null ? t("stats.beatsValue", { percent: submission.beatsPercent }) : "-"}
        />
        <StatTile label={t("stats.mode")} value={t(`mode.${submission.submissionMode}`)} />
      </div>

      {/* Khu vực D — Testcase table. Hidden entirely on COMPILE_ERROR (BD Sheet 6 Khu vực D NO 1). */}
      {!isCompileError ? (
        <div className="glass-card mb-4 overflow-hidden border border-[var(--color-border)]">
          <table className="w-full text-[13px]">
            <caption className="sr-only">{t("testcaseTableCaption")}</caption>
            <thead>
              <tr className="border-b border-[var(--color-border)] text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                <th scope="col" className="px-3 py-2 text-left">{t("col.order")}</th>
                <th scope="col" className="px-3 py-2 text-left">{t("col.visibility")}</th>
                <th scope="col" className="px-3 py-2 text-left">{t("col.verdict")}</th>
                <th scope="col" className="px-3 py-2 text-right">{t("col.runtime")}</th>
                <th scope="col" className="px-3 py-2 text-right">{t("col.memory")}</th>
                <th scope="col" className="px-3 py-2 text-right">{t("col.action")}</th>
              </tr>
            </thead>
            <tbody>
              {submission.testcases.length === 0 ? (
                <tr>
                  <td colSpan={6}>
                    <EmptyState>{t("testcaseEmpty")}</EmptyState>
                  </td>
                </tr>
              ) : (
                submission.testcases.map((tc) => (
                  <TestcaseRow
                    key={tc.testcaseId}
                    testcase={tc}
                    expanded={expanded === tc.testcaseId}
                    onToggle={() =>
                      setExpanded((prev) => (prev === tc.testcaseId ? null : tc.testcaseId))
                    }
                    t={t}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {/* Khu vực E — Code viewer */}
      <div className="glass-card mb-4 border border-[var(--color-border)] px-5 py-4">
        <div className="mb-2.5 flex items-center justify-between gap-2">
          <Badge variant="neutral">{t(`language.${submission.language}`)}</Badge>
          <Button variant="ghost" size="sm" onClick={copyCode}>
            {copied ? t("code.copied") : t("code.copy")}
          </Button>
        </div>
        <pre className="max-h-[420px] overflow-auto rounded-lg bg-[var(--color-surface-hover)] p-3 font-mono text-[12.5px] whitespace-pre-wrap">
          {submission.sourceCode}
        </pre>
        {isCompileError && submission.compileErrorMessage ? (
          <pre className="mt-3 max-h-[240px] overflow-auto rounded-lg border border-[var(--color-admin-negative)] bg-[color-mix(in_srgb,var(--color-admin-negative)_10%,transparent)] p-3 font-mono text-[12.5px] whitespace-pre-wrap text-[var(--color-admin-negative)]">
            {submission.compileErrorMessage}
          </pre>
        ) : null}
      </div>

      {/* Khu vực F — Next actions */}
      <div className="flex flex-wrap gap-2.5">
        <Button variant="cta" disabled={!canOpenAi} title={canOpenAi ? undefined : t("actions.needAccepted")} asChild={canOpenAi}>
          {canOpenAi ? (
            <Link href={`/submissions/${submission.id}/review`}>{t("actions.solutionReview")}</Link>
          ) : (
            <span>{t("actions.solutionReview")}</span>
          )}
        </Button>
        <Button variant="ghost" disabled={!canOpenAi} title={canOpenAi ? undefined : t("actions.needAccepted")} asChild={canOpenAi} className="border border-[var(--color-border)]">
          {canOpenAi ? (
            <Link href={`/submissions/${submission.id}/interview`}>{t("actions.mockInterview")}</Link>
          ) : (
            <span>{t("actions.mockInterview")}</span>
          )}
        </Button>
        <Button variant="ghost" asChild className="border border-[var(--color-border)]">
          <Link href={`/problems/${submission.problemSlug}`}>{t("actions.retry")}</Link>
        </Button>
      </div>
    </div>
  );
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="glass-card border border-[var(--color-border)] px-3.5 py-3">
      <p className="mb-1 text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
        {label}
      </p>
      <p className="font-mono text-[16px] font-bold">{value}</p>
    </div>
  );
}

function TestcaseRow({
  testcase,
  expanded,
  onToggle,
  t,
}: {
  testcase: SubmissionTestcaseResult;
  expanded: boolean;
  onToggle: () => void;
  t: ReturnType<typeof useT>;
}) {
  const isSample = testcase.visibility === "SAMPLE";
  return (
    <>
      <tr className="border-b border-[var(--color-border)] last:border-b-0">
        <td className="px-3 py-2 font-mono">{testcase.order}</td>
        <td className="px-3 py-2">
          <Badge variant={isSample ? "cyan" : "neutral"}>{t(`visibility.${testcase.visibility}`)}</Badge>
        </td>
        <td className="px-3 py-2">
          <Badge variant={TESTCASE_VERDICT_VARIANT[testcase.verdict] ?? "neutral"}>{testcase.verdict}</Badge>
        </td>
        <td className="px-3 py-2 text-right font-mono">{formatMs(testcase.runtimeMs)}</td>
        <td className="px-3 py-2 text-right font-mono">{formatMb(testcase.memoryKb)}</td>
        <td className="px-3 py-2 text-right">
          <Button variant="ghost" size="sm" disabled={!isSample} onClick={onToggle}>
            {t("col.expand")}
          </Button>
        </td>
      </tr>
      {expanded ? (
        <tr className="border-b border-[var(--color-border)] last:border-b-0 bg-[var(--color-surface-hover)]">
          <td colSpan={6} className="px-3 py-3">
            {isSample ? (
              <div className="grid gap-2 sm:grid-cols-3">
                <TestcasePane label={t("sample.input")} value={testcase.sampleInput} />
                <TestcasePane label={t("sample.expected")} value={testcase.sampleExpectedOutput} />
                <TestcasePane label={t("sample.actual")} value={testcase.sampleActualOutput} />
              </div>
            ) : (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("sample.hiddenNotice")}</p>
            )}
          </td>
        </tr>
      ) : null}
    </>
  );
}

function TestcasePane({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <p className="mb-1 text-[11px] font-semibold tracking-[0.06em] text-[var(--color-text-subtle)] uppercase">
        {label}
      </p>
      <pre className="max-h-[120px] overflow-auto rounded-md bg-[var(--color-surface)] p-2 font-mono text-[12px] whitespace-pre-wrap">
        {value ?? "-"}
      </pre>
    </div>
  );
}
