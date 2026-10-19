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

import { useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import {
  fetchSubmissionDetail,
  isSubmissionInInstructorScope,
  SUBMISSION_STATUS_VARIANT,
  type SubmissionDetail,
  type SubmissionTestcaseResult,
} from "@/entities/submission";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN, type DateLocale } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { Badge, Button, EmptyState, type BadgeVariant } from "@/shared/ui";

const TESTCASE_VERDICT_VARIANT: Record<string, BadgeVariant> = {
  AC: "success",
  WA: "negative",
  TLE: "warn",
  MLE: "warn",
  RE: "negative",
};

// BD USR0201 Q2 (owner 2026-10-18): Beats needs at least this many Accepted submissions for the same
// problem and language; below it the tile prints "not enough data" instead of a percentage.
const BEATS_MIN_ACCEPTED = 10;

function formatMs(ms: number | null): string {
  return ms == null ? "-" : `${ms} ms`;
}

function formatMb(kb: number | null): string {
  return kb == null ? "-" : `${(kb / 1024).toFixed(1)} MB`;
}

// `beatsSampleSize` (Accepted submissions behind the percentage) is not in the entity yet; the mock
// leaves it out, which counts as "enough". The real API returns it next to beatsPercent.
function beatsTileValue(
  submission: SubmissionDetail & { beatsSampleSize?: number },
  t: ReturnType<typeof useT>,
): string {
  if (submission.status !== "ACCEPTED") return "-";
  const percent = submission.beatsPercent;
  const enough =
    (submission.beatsSampleSize ?? BEATS_MIN_ACCEPTED) >= BEATS_MIN_ACCEPTED;
  return percent != null && enough
    ? t("stats.beatsValue", { percent })
    : t("stats.beatsNoData");
}

export function SubmissionResultView({
  area = "student",
  renderBreadcrumb,
  renderContext,
}: {
  /** "instructor": read-only for A2 (DEC-2026-1013), breadcrumb back to the submission list, no learner actions. */
  area?: "student" | "instructor";
  /** Instructor only: breadcrumb trail that knows the student and class the submission belongs to. */
  renderBreadcrumb?: (submission: SubmissionDetail) => ReactNode;
  /** Instructor only: context card (who submitted, shortcuts) in place of the plain read-only note. */
  renderContext?: (submission: SubmissionDetail) => ReactNode;
}) {
  const t = useT("submissionResult");
  const locale = useLocale() as DateLocale;
  const levels = useProblemLevels();
  const params = useParams<{ submissionId?: string }>();
  const submissionId = params?.submissionId ?? "";
  const submission = useMemo(
    () => fetchSubmissionDetail(submissionId),
    [submissionId],
  );
  const [expanded, setExpanded] = useState<string | null>(null);

  const isInstructor = area === "instructor";
  // Outside the instructor's classes it is a 404, same as an unknown id (no existence leak).
  if (
    !submission ||
    (isInstructor && !isSubmissionInInstructorScope(submissionId))
  ) {
    return (
      <>
        <h1 className="sr-only">{t("pageTitle")}</h1>
        <EmptyState>{t("notFound")}</EmptyState>
      </>
    );
  }

  const isCompileError = submission.status === "COMPILE_ERROR";
  const canOpenAi = submission.status === "ACCEPTED";

  function copyCode() {
    const clipboard = navigator.clipboard;
    if (!clipboard) {
      toast.error(t("code.copyFailed"));
      return;
    }
    clipboard.writeText(submission!.sourceCode).then(
      () => toast.success(t("code.copied")),
      () => toast.error(t("code.copyFailed")),
    );
  }

  return (
    <div className={isInstructor ? "flex flex-col p-6" : "p-6"}>
      <h1 className="sr-only">{t("pageTitle")}</h1>
      {/* Khu vực A — Breadcrumb */}
      <nav
        aria-label={t("breadcrumbLabel")}
        className="mb-4 flex flex-wrap items-center gap-1.5 text-[12.5px] text-[var(--color-text-muted)]"
      >
        {isInstructor && renderBreadcrumb ? (
          renderBreadcrumb(submission)
        ) : isInstructor ? (
          <>
            <Link
              href="/instructor/submissions"
              className="hover:text-[var(--color-text)]"
            >
              {t("breadcrumbSubmissions")}
            </Link>
            <span aria-hidden="true">/</span>
            <span>{submission.problemTitle}</span>
          </>
        ) : (
          <>
            <Link href="/problems" className="hover:text-[var(--color-text)]">
              {t("breadcrumbProblems")}
            </Link>
            <span aria-hidden="true">/</span>
            <Link
              href={`/problems/${submission.problemSlug}`}
              className="hover:text-[var(--color-text)]"
            >
              {submission.problemTitle}
            </Link>
          </>
        )}
        {isInstructor && renderBreadcrumb ? null : (
          <>
            <span aria-hidden="true">/</span>
            <span>{t("breadcrumbCurrent", { id: submission.id })}</span>
          </>
        )}
      </nav>

      {isInstructor && renderContext ? (
        renderContext(submission)
      ) : isInstructor ? (
        <p className="glass-card mb-4 border border-[var(--color-border)] px-4 py-2.5 text-[12.5px] text-[var(--color-text-muted)]">
          {t("instructorReadOnly")}
        </p>
      ) : null}

      {/* Khu vực B — Verdict banner */}
      <div className="glass-card mb-4 border border-[var(--color-border)] px-5 py-4">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant={SUBMISSION_STATUS_VARIANT[submission.status]}>
            {t(`verdict.${submission.status}`)}
          </Badge>
          <Badge variant={problemLevelTone(levels, submission.difficulty)}>
            {problemLevelLabel(levels, submission.difficulty)}
          </Badge>
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
            submittedAt: formatDateVN(submission.submittedAt, {
              locale,
              withTime: true,
            }),
          })}
        </p>
        {/* Khu vực C — run stats as one text line (owner rule 2026-10-13: no KPI strip on a detail screen). */}
        <p className="mt-1 font-mono text-[12.5px] text-[var(--color-text-muted)]">
          {[
            `${t("stats.runtime")} ${formatMs(submission.runtimeMs)}`,
            `${t("stats.memory")} ${formatMb(submission.memoryKb)}`,
            ...(isInstructor
              ? []
              : [`${t("stats.beats")} ${beatsTileValue(submission, t)}`]),
            t(`mode.${submission.submissionMode}`),
          ].join(" · ")}
        </p>
      </div>

      {/* Khu vực D — Testcase table. Hidden entirely on COMPILE_ERROR (BD Sheet 6 Khu vực D NO 1). */}
      {!isCompileError ? (
        <div
          className={
            isInstructor
              ? "glass-card order-3 mb-4 overflow-hidden border border-[var(--color-border)]"
              : "glass-card mb-4 overflow-hidden border border-[var(--color-border)]"
          }
        >
          <table className="w-full text-[13px]">
            <caption className="sr-only">{t("testcaseTableCaption")}</caption>
            <thead>
              <tr className="border-b border-[var(--color-border)] text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                <th scope="col" className="px-3 py-2 text-left">
                  {t("col.order")}
                </th>
                <th scope="col" className="px-3 py-2 text-left">
                  {t("col.visibility")}
                </th>
                <th scope="col" className="px-3 py-2 text-left">
                  {t("col.verdict")}
                </th>
                <th scope="col" className="px-3 py-2 text-right">
                  {t("col.runtime")}
                </th>
                <th scope="col" className="px-3 py-2 text-right">
                  {t("col.memory")}
                </th>
                <th scope="col" className="px-3 py-2 text-right">
                  {t("col.action")}
                </th>
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
                      setExpanded((prev) =>
                        prev === tc.testcaseId ? null : tc.testcaseId,
                      )
                    }
                    t={t}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {/* Khu vực E — Code viewer. Instructor: first block after the banner, the code is what they came for. */}
      <div
        className={
          isInstructor
            ? "glass-card order-2 mb-4 border border-[var(--color-border)] px-5 py-4"
            : "glass-card mb-4 border border-[var(--color-border)] px-5 py-4"
        }
      >
        <div className="mb-2.5 flex items-center justify-between gap-2">
          <Badge variant="neutral">
            {t(`language.${submission.language}`)}
          </Badge>
          <Button variant="ghost" size="sm" onClick={copyCode}>
            {t("code.copy")}
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

      {/* Khu vực F — Next actions. Learner-only: the instructor reads, never retries or opens the AI flows. */}
      {isInstructor ? null : (
        <div className="flex flex-wrap gap-2.5">
          <Button
            variant="cta"
            disabled={!canOpenAi}
            title={canOpenAi ? undefined : t("actions.needAccepted")}
            asChild={canOpenAi}
          >
            {canOpenAi ? (
              <Link href={`/submissions/${submission.id}/review`}>
                {t("actions.solutionReview")}
              </Link>
            ) : (
              <span>{t("actions.solutionReview")}</span>
            )}
          </Button>
          <Button
            variant="ghost"
            disabled={!canOpenAi}
            title={canOpenAi ? undefined : t("actions.needAccepted")}
            asChild={canOpenAi}
            className="border border-[var(--color-border)]"
          >
            {canOpenAi ? (
              <Link href={`/submissions/${submission.id}/interview`}>
                {t("actions.mockInterview")}
              </Link>
            ) : (
              <span>{t("actions.mockInterview")}</span>
            )}
          </Button>
          <Button
            variant="ghost"
            asChild
            className="border border-[var(--color-border)]"
          >
            <Link href={`/problems/${submission.problemSlug}`}>
              {t("actions.retry")}
            </Link>
          </Button>
        </div>
      )}
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
          <Badge variant={isSample ? "cyan" : "neutral"}>
            {t(`visibility.${testcase.visibility}`)}
          </Badge>
        </td>
        <td className="px-3 py-2">
          <Badge
            variant={TESTCASE_VERDICT_VARIANT[testcase.verdict] ?? "neutral"}
          >
            {testcase.verdict}
          </Badge>
        </td>
        <td className="px-3 py-2 text-right font-mono">
          {formatMs(testcase.runtimeMs)}
        </td>
        <td className="px-3 py-2 text-right font-mono">
          {formatMb(testcase.memoryKb)}
        </td>
        <td className="px-3 py-2 text-right">
          <Button
            variant="ghost"
            size="sm"
            disabled={!isSample}
            aria-expanded={isSample ? expanded : undefined}
            onClick={onToggle}
          >
            {t("col.expand")}
          </Button>
        </td>
      </tr>
      {expanded ? (
        <tr className="border-b border-[var(--color-border)] last:border-b-0 bg-[var(--color-surface-hover)]">
          <td colSpan={6} className="px-3 py-3">
            <div className="grid gap-2 sm:grid-cols-3">
              <TestcasePane
                label={t("sample.input")}
                value={testcase.sampleInput}
              />
              <TestcasePane
                label={t("sample.expected")}
                value={testcase.sampleExpectedOutput}
              />
              <TestcasePane
                label={t("sample.actual")}
                value={testcase.sampleActualOutput}
              />
            </div>
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
