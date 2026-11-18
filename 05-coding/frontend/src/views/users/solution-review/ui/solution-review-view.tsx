// USR0301 Solution Review (DD 03-dd/screens/users/USR0301_solution_review.md). Real mode: the report
// comes from F5 `POST /ai/submissions/{id}/solution-review` (one idempotent call, can take up to 30 s,
// no automatic retry), the problem title and the submitted code from F4 `GET /judge/submissions/{id}`.
// Mock mode keeps the three `?demo=` outcomes of the prototype and brings its own source block.
//
// Layout follows 09-layoutBase/Phân tích bài giải.dc.html: breadcrumb + top stats (:99-116), a
// minmax(0,1fr) 380px grid (:119) holding the edu banner / complexity table / strengths-improvements
// / diff (left) and submitted-code / review-topics / apply CTA (right).
//
// No Monaco library is installed yet, so the read-only Diff Viewer (Sheet 4.4 area G) computes the
// diff from the original vs suggested code with entities/solution-review `lineDiff` (the DB stores
// no diff text, 02-bd/database/F5_ai-review.md 1.3a) and renders a plain coloured list.
"use client";

import { Suspense, useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  AIR_BUDGET_LOCKED,
  AIR_COOLDOWN,
  AIR_NOT_ACCEPTED,
  AIR_NOT_OWNER,
  AIR_QUOTA_EXCEEDED,
  AIR_SUBMISSION_NOT_FOUND,
  lineDiff,
  useSolutionReview,
  type ReviewSource,
  type SolutionReviewDemoOutcome,
  type SolutionReviewDetail,
} from "@/entities/solution-review";
import { useSubmissionDetail } from "@/entities/submission";
import { ApiError } from "@/shared/api";
import {
  problemLevelLabel,
  saveWorkspaceDraft,
  useProblemLevels,
} from "@/entities/problem";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import { formatDateVN, type DateLocale } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  NoticeTile,
  Skeleton,
  SeeAllLink,
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
    <Suspense fallback={<LoadingSkeleton />}>
      <SolutionReviewContent />
    </Suspense>
  );
}

function SolutionReviewContent() {
  const t = useT("solutionReview");
  const router = useRouter();
  const params = useParams<{ submissionId?: string }>();
  const submissionId = params?.submissionId ?? "";
  const searchParams = useSearchParams();
  const rawDemo = searchParams.get("demo");
  const demo: SolutionReviewDemoOutcome = isDemoOutcome(rawDemo)
    ? rawDemo
    : "ready";

  const query = useSolutionReview(submissionId, demo);
  const submission = useSubmissionDetail(submissionId);
  const apiMessage = useApiErrorMessage();
  const [applyConfirmOpen, setApplyConfirmOpen] = useState(false);

  // isFetching (not isLoading) so Retry from the error state shows the skeleton again and cannot be
  // double-clicked (EVT-7).
  const isLoading = query.isFetching;
  const code = query.error instanceof ApiError ? query.error.code : undefined;
  const isBudgetLocked = code === AIR_BUDGET_LOCKED;
  // One merged notice for 101 / 102 / 302 on purpose: it must not tell whose submission it is.
  const isUnavailable =
    code === AIR_SUBMISSION_NOT_FOUND ||
    code === AIR_NOT_ACCEPTED ||
    code === AIR_NOT_OWNER;
  const isRateLimited = code === AIR_QUOTA_EXCEEDED || code === AIR_COOLDOWN;
  const retryAfter = retryAfterSeconds(query.error);
  const isTransientError =
    query.isError && !isBudgetLocked && !isUnavailable && !isRateLimited;
  const data = query.data as SolutionReviewDetail | undefined;
  const source = data ? sourceOf(data, submission) : null;

  // The error blocks below replace the report; the toast announces the failure at the moment it
  // happens (errorUpdatedAt changes on every failed attempt, so a failed retry toasts again).
  const errorUpdatedAt = query.errorUpdatedAt;
  useEffect(() => {
    if (!errorUpdatedAt) return;
    if (isBudgetLocked) toast.error(t("toast.budgetLocked"));
    else if (isUnavailable) toast.error(t("toast.unavailable"));
    else if (isRateLimited)
      toast.error(t("rateLimitedToast", { seconds: retryAfter }));
    else if (isTransientError)
      toast.error(
        code === undefined || code.startsWith("AIR-")
          ? t("toast.transientError")
          : apiMessage(query.error),
      );
    // eslint-disable-next-line react-hooks/exhaustive-deps -- t is stable per locale; only a new failure should toast
  }, [errorUpdatedAt]);

  return (
    <section className="p-6">
      <nav
        aria-label={t("breadcrumbLabel")}
        className="mb-4 flex items-center gap-2 text-[13px]"
      >
        <Link
          href={`/submissions/${submissionId}`}
          className="font-semibold text-[var(--color-text-muted)]"
        >
          {t("breadcrumbSubmissionResult")}
        </Link>
        <span aria-hidden="true">/</span>
        <Link
          href={`/submissions/${submissionId}`}
          className="font-mono text-[var(--color-text-muted)]"
        >
          {submissionId}
        </Link>
        <span aria-hidden="true">/</span>
        <span className="text-[var(--color-text)]">
          {t("breadcrumbCurrent")}
        </span>
      </nav>

      {isLoading ? <LoadingSkeleton /> : null}

      {!isLoading && isTransientError ? (
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

      {!isLoading && isRateLimited ? (
        <NoticeTile tone="warn" title={t("errorRateLimitedTitle")}>
          {t("errorRateLimitedBody", { seconds: retryAfter })}
          <RetryCountdown
            key={errorUpdatedAt}
            seconds={retryAfter}
            onRetry={() => query.refetch()}
            t={t}
          />
        </NoticeTile>
      ) : null}

      {!isLoading && isUnavailable ? (
        <NoticeTile tone="warn" title={t("errorUnavailableTitle")}>
          {t("errorUnavailableBody")}
          <Link
            href="/submissions"
            className="mt-3 block font-semibold text-[var(--color-primary)] underline"
          >
            {t("backToSubmissions")}
          </Link>
        </NoticeTile>
      ) : null}

      {!isLoading && isBudgetLocked ? (
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
          source={source}
          sourceFailed={submission.isError || (submission.isSuccess && !source)}
          t={t}
          onApplyClick={() => setApplyConfirmOpen(true)}
        />
      ) : null}

      <ConfirmDialog
        open={applyConfirmOpen}
        onClose={() => setApplyConfirmOpen(false)}
        onConfirm={() => {
          setApplyConfirmOpen(false);
          if (!data || !source) return;
          // EVT-6: the suggestion becomes the browser draft of that problem, then the workspace opens
          // on it. The previous-draft history entry from BD is not built yet (PROTOTYPE_DEBT).
          const suggested = data.codeDiff.suggestedCode;
          if (suggested === null) return;
          const ok = saveWorkspaceDraft(
            data.problemId,
            data.languageKey,
            source.submissionModel,
            suggested,
          );
          if (!ok) {
            toast.error(t("toast.applyFailed"));
            return;
          }
          toast.success(t("toast.applied"));
          router.push(`/workspace/${data.problemId}`);
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
  const t = useT("solutionReview");
  return (
    <div className="flex flex-col gap-3" role="status" aria-busy="true">
      <span className="sr-only">{t("loadingLabel")}</span>
      <Skeleton className="h-6 w-64" />
      <Skeleton className="h-40 w-full" />
      <Skeleton className="h-56 w-full" />
    </div>
  );
}

function ReadyReport({
  data,
  source,
  sourceFailed,
  t,
  onApplyClick,
}: {
  data: SolutionReviewDetail;
  source: ReviewSource | null;
  /** F4 failed or returned no problem: the report still shows, code and diff are replaced by a note. */
  sourceFailed: boolean;
  t: ReturnType<typeof useT>;
  onApplyClick: () => void;
}) {
  const levelList = useProblemLevels();
  const locale = useLocale() as DateLocale;
  return (
    <>
      <div className="mb-4 flex flex-wrap items-center gap-5">
        <Stat
          label={t("statProblem")}
          value={source?.problemTitle ?? "—"}
          unit={source ? problemLevelLabel(levelList, source.difficulty) : "—"}
        />
        {source?.withdrawn ? (
          <Badge variant="neutral">{t("withdrawn")}</Badge>
        ) : null}
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
        <Stat
          label={t("statCreatedAt")}
          value={formatDateVN(data.createdAt, { locale, withTime: true })}
          unit="UTC+7"
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
                      {data.complexity.optimalTime ?? "—"}
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
                      {data.complexity.optimalSpace ?? "—"}
                    </td>
                    <td className="py-2 text-[var(--color-text-muted)]">
                      {data.complexity.spaceExplanation}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>

          <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
            <Card title={t("strengthsTitle")}>
              <ul className="flex flex-col gap-2.5 text-[13.5px]">
                {data.strengths.map((item, index) => (
                  <li key={`${index}-${item}`} className="flex gap-2.5">
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
                {data.improvements.map((item, index) => (
                  <li key={`${index}-${item}`} className="flex gap-2.5">
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
              {data.edgeCases.map((item, index) => (
                <li key={`${index}-${item}`}>{item}</li>
              ))}
            </ul>
          </Card>

          {data.followUpTopics.length > 0 ? (
            // F5-06: topics only, no question ids (ai-review never reads interview-bank). The question
            // bank list takes no search parameter yet, so the cta opens it unfiltered.
            <Card title={t("followUpTitle")}>
              <ul className="flex flex-wrap gap-2">
                {data.followUpTopics.map((topic, index) => (
                  <li
                    key={`${index}-${topic}`}
                    className="rounded-full bg-[var(--color-track)] px-3 py-1 text-[12.5px]"
                  >
                    {topic}
                  </li>
                ))}
              </ul>
              <SeeAllLink href="/interview-bank">{t("followUpCta")}</SeeAllLink>
            </Card>
          ) : null}

          {data.codeDiff.suggestedCode !== null && !source && sourceFailed ? (
            <NoticeTile tone="info" title={t("diffTitle")}>
              {t("sourceUnavailable")}
            </NoticeTile>
          ) : null}

          {data.codeDiff.suggestedCode !== null && source ? (
            <Card title={t("diffTitle")}>
              <pre
                role="group"
                aria-label={t("diffTitle")}
                className="overflow-x-auto rounded-xl bg-[var(--color-track)] p-3.5 font-mono text-[12.5px] leading-[1.8]"
              >
                {lineDiff(
                  source.sourceCode.map(trimEnd),
                  splitLines(data.codeDiff.suggestedCode).map(trimEnd),
                ).map((line, index) => (
                  <div key={index} className={DIFF_LINE_CLASS[line.kind]}>
                    <span className="mr-3 inline-block w-2.5 text-center select-none">
                      {DIFF_SIGN[line.kind]}
                    </span>
                    {line.kind === "same" ? null : (
                      <span className="sr-only">
                        {t(line.kind === "add" ? "diff.added" : "diff.removed")}
                      </span>
                    )}
                    {line.text}
                  </div>
                ))}
              </pre>
              {data.codeDiff.explanation ? (
                <p className="mt-2.5 text-[12.5px] text-[var(--color-text-muted)]">
                  {data.codeDiff.explanation}
                </p>
              ) : null}
            </Card>
          ) : null}
        </div>

        <div className="flex flex-col gap-3.5">
          {source ? (
            <Card
              title={t("sourceCodeTitle")}
              description={t(`language.${data.languageKey}`)}
            >
              <pre className="overflow-x-auto rounded-xl bg-[var(--color-track)] p-3.5 font-mono text-xs leading-[1.8]">
                {source.sourceCode.map((line, index) => (
                  <div key={index}>
                    <span className="mr-3 inline-block w-4 text-right text-[var(--color-text-subtle)] select-none">
                      {index + 1}
                    </span>
                    {line}
                  </div>
                ))}
              </pre>
            </Card>
          ) : sourceFailed ? (
            <NoticeTile tone="info" title={t("sourceCodeTitle")}>
              {t("sourceUnavailable")}
            </NoticeTile>
          ) : (
            <Skeleton className="h-40 w-full" />
          )}

          {data.codeDiff.suggestedCode !== null ? (
            <Button
              variant="ghost"
              size="md"
              className="w-full border border-[var(--color-border)]"
              disabled={!source}
              onClick={onApplyClick}
            >
              {t("applyCta")}
            </Button>
          ) : null}
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

const trimEnd = (line: string) => line.trimEnd();

/** Splits on CR LF or LF and drops the empty last line a trailing newline leaves. */
function splitLines(text: string): string[] {
  const lines = text.split(/\r?\n/);
  if (lines.length > 1 && lines[lines.length - 1] === "") lines.pop();
  return lines;
}

/** `details.retryAfterSeconds` of AIR-402 / AIR-404; 20 s when the server did not send it. */
function retryAfterSeconds(error: unknown): number {
  const value =
    error instanceof ApiError ? error.details?.retryAfterSeconds : undefined;
  return typeof value === "number" && value > 0 ? Math.ceil(value) : 20;
}

type SubmissionQuery = ReturnType<typeof useSubmissionDetail>;

/** Mock carries its own source block; real mode joins F4's answer (null while loading or failed). */
function sourceOf(
  data: SolutionReviewDetail,
  submission: SubmissionQuery,
): ReviewSource | null {
  if (data.source) return data.source;
  const detail = submission.data;
  // No problem in the F4 answer (F2 failed): the title and the model are unknown, so no source block.
  if (!detail || !detail.problemId) return null;
  return {
    problemTitle: detail.problemTitle,
    difficulty: detail.difficulty,
    withdrawn: detail.problemWithdrawn === true,
    submissionModel:
      detail.submissionMode === "STANDARD_IO" ? "stdio" : "function",
    sourceCode: splitLines(detail.sourceCode),
  };
}

/** Disabled Retry until the server's wait is over; the countdown is not in an aria-live region. */
function RetryCountdown({
  seconds,
  onRetry,
  t,
}: {
  seconds: number;
  onRetry: () => void;
  t: ReturnType<typeof useT>;
}) {
  const [left, setLeft] = useState(seconds);
  useEffect(() => {
    if (left <= 0) return;
    const timer = setTimeout(() => setLeft((value) => value - 1), 1000);
    return () => clearTimeout(timer);
  }, [left]);
  return (
    <Button
      variant="ghost"
      size="sm"
      className="mt-3"
      disabled={left > 0}
      onClick={onRetry}
    >
      {left > 0 ? t("retryIn", { seconds: left }) : t("retry")}
    </Button>
  );
}
