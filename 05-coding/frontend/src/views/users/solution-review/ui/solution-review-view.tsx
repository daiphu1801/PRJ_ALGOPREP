// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Layout follows 09-layoutBase/Phân tích bài giải.dc.html: breadcrumb + top stats (:99-116), a
// minmax(0,1fr) 380px grid (:119) holding the edu banner / complexity table / strengths-improvements
// / diff (left) and submitted-code / review-topics / apply CTA (right).
//
// Three mutually exclusive states from BD Sheet 5 Khu vực I (loading / errorTransient /
// errorBudgetLocked) have no real backend to drive them yet — read from `?demo=` the same way the
// admin_overview debt row proposes (PROTOTYPE_DEBT.md section 9: "bật cơ chế demo lỗi/rỗng thật qua
// query param debug"). Retry (EVT-7) re-runs the same deterministic outcome rather than "fixing"
// itself — there is no real AI call behind it yet.
//
// No diff/Monaco library is added for the read-only Diff Viewer (Sheet 4.4 area G) — the mock data
// already carries same/add/del per line (ported from the mockup's own DIFF array), so a plain
// coloured list renders the same information without a new dependency.
"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  AI_BUDGET_LOCKED_CODE,
  RUBRIC_CRITERION_CODES,
  useSolutionReview,
  type SolutionReviewDemoOutcome,
  type SolutionReviewDetail,
} from "@/entities/solution-review";
import { ApiError } from "@/shared/api";
import { problemLevelLabel, useProblemLevels } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  NoticeTile,
  Skeleton,
} from "@/shared/ui";

const DIFF_LINE_CLASS: Record<string, string> = {
  same: "text-[var(--color-text-muted)]",
  add: "text-[var(--color-success-text)] bg-[color-mix(in_srgb,var(--color-success)_10%,transparent)]",
  del: "text-[var(--color-admin-negative)] bg-[color-mix(in_srgb,var(--color-admin-negative)_8%,transparent)]",
};

const DIFF_SIGN: Record<string, string> = { same: " ", add: "+", del: "−" };

function isDemoOutcome(
  value: string | null,
): value is SolutionReviewDemoOutcome {
  return (
    value === "ready" ||
    value === "errorTransient" ||
    value === "errorBudgetLocked"
  );
}

export function SolutionReviewView() {
  // useSearchParams requires a Suspense boundary in the App Router (see views/auth/ui/auth-view.tsx).
  return (
    <Suspense fallback={null}>
      <SolutionReviewContent />
    </Suspense>
  );
}

function SolutionReviewContent() {
  const t = useT("solutionReview");
  const searchParams = useSearchParams();
  const rawDemo = searchParams.get("demo");
  const demo: SolutionReviewDemoOutcome = isDemoOutcome(rawDemo)
    ? rawDemo
    : "ready";

  const query = useSolutionReview(demo);
  const [applyConfirmOpen, setApplyConfirmOpen] = useState(false);

  const isLoading = query.isLoading;
  const isBudgetLocked =
    query.error instanceof ApiError &&
    query.error.code === AI_BUDGET_LOCKED_CODE;
  const isTransientError = query.isError && !isBudgetLocked;
  const data = query.data as SolutionReviewDetail | undefined;

  // The error blocks below replace the report; the toast announces the failure at the moment it
  // happens (errorUpdatedAt changes on every failed attempt, so a failed retry toasts again).
  const errorUpdatedAt = query.errorUpdatedAt;
  useEffect(() => {
    if (!errorUpdatedAt) return;
    if (isBudgetLocked) toast.error(t("toast.budgetLocked"));
    else if (isTransientError) toast.error(t("toast.transientError"));
    // eslint-disable-next-line react-hooks/exhaustive-deps -- t is stable per locale; only a new failure should toast
  }, [errorUpdatedAt]);

  return (
    <section className="p-6">
      <nav
        aria-label={t("breadcrumbLabel")}
        className="mb-4 flex items-center gap-2 text-[13px]"
      >
        <Link
          href="/submissions"
          className="font-semibold text-[var(--color-text-muted)]"
        >
          {t("breadcrumbSubmissionResult")}
        </Link>
        <span aria-hidden="true">/</span>
        <span className="font-mono text-[var(--color-text-muted)]">
          {data?.submissionId ?? "SUB-2841"}
        </span>
        <span aria-hidden="true">/</span>
        <span className="text-[var(--color-text)]">
          {t("breadcrumbCurrent")}
        </span>
      </nav>

      {isLoading ? <LoadingSkeleton /> : null}

      {isTransientError ? (
        <NoticeTile tone="warn" title={t("errorTransientTitle")}>
          {t("errorTransientBody")}
          <Button
            variant="ghost"
            size="sm"
            className="mt-3"
            onClick={() => query.refetch()}
          >
            {t("retry")}
          </Button>
        </NoticeTile>
      ) : null}

      {isBudgetLocked ? (
        <NoticeTile tone="negative" title={t("errorBudgetLockedTitle")}>
          {t("errorBudgetLockedBody")}
          <Link
            href="/problems"
            className="mt-3 block font-semibold text-[var(--color-primary)] underline"
          >
            {t("backToWorkspace")}
          </Link>
        </NoticeTile>
      ) : null}

      {!isLoading && !query.isError && data ? (
        <ReadyReport
          data={data}
          t={t}
          onApplyClick={() => setApplyConfirmOpen(true)}
        />
      ) : null}

      <ConfirmDialog
        open={applyConfirmOpen}
        onClose={() => setApplyConfirmOpen(false)}
        onConfirm={() => {
          setApplyConfirmOpen(false);
          toast.success(t("toast.applied"));
        }}
        title={t("applyConfirmTitle")}
        confirmLabel={t("applyConfirmConfirm")}
        cancelLabel={t("applyConfirmCancel")}
      >
        {t("applyConfirmBody")}
      </ConfirmDialog>
    </section>
  );
}

function LoadingSkeleton() {
  return (
    <div className="flex flex-col gap-3" role="status" aria-busy="true">
      <Skeleton className="h-6 w-64" />
      <Skeleton className="h-40 w-full" />
      <Skeleton className="h-56 w-full" />
    </div>
  );
}

function ReadyReport({
  data,
  t,
  onApplyClick,
}: {
  data: SolutionReviewDetail;
  t: ReturnType<typeof useT>;
  onApplyClick: () => void;
}) {
  const levelList = useProblemLevels();
  return (
    <>
      <div className="mb-4 flex flex-wrap items-center gap-5">
        <Stat
          label={t("statProblem")}
          value={data.problemTitle}
          unit={problemLevelLabel(levelList, data.difficulty)}
        />
        <Stat
          label={t("statApproach")}
          value={
            data.isApproachOptimal
              ? t("approachOptimal")
              : t("approachSuboptimal")
          }
          unit={data.approachTitle}
        />
        <Stat
          label={t("statReadability")}
          value={String(data.readabilityScore)}
          unit="/ 5"
        />
        <Button variant="cta" size="sm" className="ml-auto" asChild>
          <Link href={`/submissions/${data.submissionId}/interview`}>
            {t("interviewCta")}
          </Link>
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-[minmax(0,1fr)_380px] lg:items-start">
        <div className="flex min-w-0 flex-col gap-3.5">
          <NoticeTile tone="info" title={t("eduNoticeTitle")}>
            {t("eduNoticeBody")}
          </NoticeTile>

          <Card title={t("summaryTitle")}>
            <p className="text-[15px] leading-relaxed text-pretty">
              {data.summaryText}
            </p>
          </Card>

          <Card title={t("complexityTitle")}>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[13px]">
                <thead>
                  <tr className="text-[11.5px] font-semibold tracking-[0.04em] text-[var(--color-text-subtle)] uppercase">
                    <th className="pb-2"></th>
                    <th className="pb-2">{t("complexityYours")}</th>
                    <th className="pb-2">{t("complexityBest")}</th>
                    <th className="pb-2">{t("complexityNote")}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-t border-[var(--color-border)]">
                    <td className="py-2 font-semibold">
                      {t("complexityTime")}
                    </td>
                    <td className="py-2 font-mono font-semibold text-[var(--color-success-text)]">
                      {data.complexity.actualTime}
                    </td>
                    <td className="py-2 font-mono text-[var(--color-text-muted)]">
                      {data.complexity.optimalTime}
                    </td>
                    <td className="py-2 text-[var(--color-text-muted)]">
                      {data.complexity.timeExplanation}
                    </td>
                  </tr>
                  <tr className="border-t border-[var(--color-border)]">
                    <td className="py-2 font-semibold">
                      {t("complexitySpace")}
                    </td>
                    <td className="py-2 font-mono font-semibold text-[var(--color-success-text)]">
                      {data.complexity.actualSpace}
                    </td>
                    <td className="py-2 font-mono text-[var(--color-text-muted)]">
                      {data.complexity.optimalSpace}
                    </td>
                    <td className="py-2 text-[var(--color-text-muted)]">
                      {data.complexity.spaceExplanation}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>

          <Card title={t("rubricTitle")}>
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              {RUBRIC_CRITERION_CODES.map((code) => {
                const criterion = data.criteriaScores.find(
                  (c) => c.code === code,
                );
                if (!criterion) return null;
                return (
                  <div
                    key={code}
                    className="rounded-xl border border-[var(--color-border)] px-3 py-2.5"
                  >
                    <p className="mb-1 text-[13px] font-semibold">
                      {t(`rubric.${code}`)}
                    </p>
                    <p className="text-[12.5px] text-[var(--color-text-muted)]">
                      {criterion.feedback}
                    </p>
                  </div>
                );
              })}
            </div>
          </Card>

          <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
            <Card title={t("strengthsTitle")}>
              <ul className="flex flex-col gap-2.5 text-[13.5px]">
                {data.strengths.map((item) => (
                  <li key={item} className="flex gap-2.5">
                    <span
                      aria-hidden="true"
                      className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-success)]"
                    />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </Card>
            <Card title={t("improvementsTitle")}>
              <ul className="flex flex-col gap-2.5 text-[13.5px]">
                {data.improvements.map((item) => (
                  <li key={item} className="flex gap-2.5">
                    <span
                      aria-hidden="true"
                      className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-admin-warn)]"
                    />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </Card>
          </div>

          <Card title={t("edgeCasesTitle")}>
            <ul className="flex flex-col gap-2 text-[13.5px]">
              {data.edgeCases.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </Card>

          <Card title={t("diffTitle")} description={data.codeDiff.diffNote}>
            <pre className="overflow-x-auto rounded-xl bg-[var(--color-track)] p-3.5 font-mono text-[12.5px] leading-[1.8]">
              {data.codeDiff.diffLines.map((line, index) => (
                <div key={index} className={DIFF_LINE_CLASS[line.kind]}>
                  <span className="mr-3 inline-block w-2.5 text-center select-none">
                    {DIFF_SIGN[line.kind]}
                  </span>
                  {line.text}
                </div>
              ))}
            </pre>
            <p className="mt-2.5 text-[12.5px] text-[var(--color-text-muted)]">
              {data.codeDiff.explanation}
            </p>
          </Card>
        </div>

        <div className="flex flex-col gap-3.5">
          <Card title={t("sourceCodeTitle")} description={data.language}>
            <pre className="overflow-x-auto rounded-xl bg-[var(--color-track)] p-3.5 font-mono text-xs leading-[1.8]">
              {data.sourceCode.map((line, index) => (
                <div key={index}>
                  <span className="mr-3 inline-block w-4 text-right text-[var(--color-text-subtle)] select-none">
                    {index + 1}
                  </span>
                  {line}
                </div>
              ))}
            </pre>
          </Card>

          <Button
            variant="ghost"
            size="md"
            className="w-full border border-[var(--color-border)]"
            onClick={onApplyClick}
          >
            {t("applyCta")}
          </Button>
        </div>
      </div>
    </>
  );
}

function Stat({
  label,
  value,
  unit,
}: {
  label: string;
  value: string;
  unit: string;
}) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
        {label}
      </span>
      <span className="font-mono text-sm font-semibold">{value}</span>
      <Badge variant="neutral">{unit}</Badge>
    </div>
  );
}
