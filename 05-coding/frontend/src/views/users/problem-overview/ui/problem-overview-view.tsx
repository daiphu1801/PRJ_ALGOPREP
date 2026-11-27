// USR0104: đọc đề và ví dụ trước khi mở workspace (/workspace/[problemId]).
"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Bookmark, Code2 } from "lucide-react";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemDetail,
  useProblemLevels,
  useSetBookmark,
  type ProblemDetail,
  type SubmissionLanguage,
} from "@/entities/problem";
import { useReviewQuickInsight } from "@/entities/solution-review";
import { useSession } from "@/entities/user";
import { LoginPromptDialog, useLoginPrompt } from "@/features/login-prompt";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  MarkdownPreview,
  QueryBoundary,
} from "@/shared/ui";

const LANGUAGES: SubmissionLanguage[] = ["java", "cpp", "python"];
const RECENT_SUBMISSION_LIMIT = 3;
const SOLVE_BUTTON_CLASS =
  "w-full bg-none bg-[var(--color-accent-blue)] [.dark_&]:bg-[var(--color-primary)]";

export function ProblemOverviewView() {
  const params = useParams<{ problemId?: string }>();
  const problemId = params?.problemId;
  const query = useProblemDetail(problemId);

  // Unknown id reads as "not found", never as "no permission" (USR0102 BD Sheet 9 NO 2).
  if (!problemId) return <NotFound />;
  return (
    <QueryBoundary query={query}>
      {(detail) =>
        detail ? (
          <ProblemOverview
            key={problemId}
            problemId={problemId}
            detail={detail}
          />
        ) : (
          <NotFound />
        )
      }
    </QueryBoundary>
  );
}

function NotFound() {
  const td = useT("problemDetail");
  return (
    <div className="p-6">
      <EmptyState>{td("empty.title")}</EmptyState>
      <p className="mt-3 text-[12.5px] text-[var(--color-text-muted)]">
        <Link href="/problems" className="underline">
          {td("empty.linkList")}
        </Link>
      </p>
    </div>
  );
}

function ProblemOverview({
  problemId,
  detail,
}: {
  problemId: string;
  detail: ProblemDetail;
}) {
  const t = useT("problemOverview");
  const td = useT("problemDetail");
  const locale = useLocale() === "en" ? "en" : "vi";
  const router = useRouter();
  const levels = useProblemLevels();
  // Khách (DEC-2026-1126, 03-dd USR0104 mục 4A): đọc đề, Giải bài và Lưu bài mở hộp thoại nhắc đăng nhập,
  // ẩn bảng giới hạn, thẻ bài nộp gần đây và thẻ Solution Review (dữ liệu cần phiên).
  const guest = useSession() === "guest";
  const prompt = useLoginPrompt();
  const [bookmarkState, setBookmarkState] = useState({
    source: detail.saved,
    value: detail.saved,
  });
  if (bookmarkState.source !== detail.saved) {
    setBookmarkState({ source: detail.saved, value: detail.saved });
  }
  const saved =
    bookmarkState.source === detail.saved ? bookmarkState.value : detail.saved;
  const setBookmark = useSetBookmark();
  const apiMessage = useApiErrorMessage();

  // Pessimistic (#11): the flag flips only after the server confirmed the wanted state. Undo sends
  // `true` again; the note of the removed bookmark is gone with it (DD 12.5), so undo restores no note.
  function setBookmarked(next: boolean) {
    setBookmark.mutate(
      { problemId, bookmarked: next },
      {
        onSuccess: () => {
          setBookmarkState({ source: detail.saved, value: next });
          if (next) {
            toast.success(td("toast.saved"), {
              label: td("toast.openSaved"),
              onClick: () => router.push("/saved"),
            });
          } else {
            toast.info(td("toast.unsaved"), {
              label: t("btnUndo"),
              onClick: () => setBookmarked(true),
            });
          }
        },
        onError: (error) => toast.error(apiMessage(error)),
      },
    );
  }

  function toggleSaved() {
    setBookmarked(!saved);
  }

  function copy(text: string) {
    const clipboard = navigator.clipboard;
    if (!clipboard) {
      toast.error(t("copyFailed"));
      return;
    }
    clipboard.writeText(text).then(
      () => toast.success(t("copied")),
      () => toast.error(t("copyFailed")),
    );
  }

  // Newest first (USR0104 Q5, owner 2026-10-21); the review link points at the latest Accepted one.
  const sorted = [...(detail.mySubmissions ?? [])].sort((a, b) =>
    b.submittedAt.localeCompare(a.submittedAt),
  );
  const recent = sorted.slice(0, RECENT_SUBMISSION_LIMIT);
  const reviewSubmission = sorted.find((item) => item.verdict === "accepted");
  // F5 #6 (real mode only): fills the card the real problem endpoint leaves null; link to its submission.
  const insight = useReviewQuickInsight(problemId).data;
  const solutionReview =
    detail.solutionReview ??
    (insight
      ? {
          timeComplexity: insight.timeComplexity,
          spaceComplexity: insight.spaceComplexity,
        }
      : null);
  const reviewSubmissionId = insight?.submissionId ?? reviewSubmission?.id;

  return (
    <div className="p-4 sm:p-6">
      <header className="mb-5">
        <Link
          href="/problems"
          className="inline-flex min-h-9 items-center gap-2 rounded-sm text-sm font-semibold text-[var(--color-accent-blue)] hover:underline focus-visible:outline-2 focus-visible:outline-offset-2"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          {t("btnBack")}
        </Link>
        <h1 className="mt-1 break-words text-3xl font-bold tracking-tight sm:text-4xl">
          {detail.title}
        </h1>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="mr-1 font-mono text-sm text-[var(--color-text-muted)]">
            {detail.code}
          </span>
          <Badge variant={problemLevelTone(levels, detail.difficulty)}>
            {problemLevelLabel(levels, detail.difficulty)}
          </Badge>
          {detail.topics.map((topic) => (
            <Badge
              key={topic}
              variant="neutral"
              className="max-w-full break-words"
            >
              {topic}
            </Badge>
          ))}
        </div>
      </header>

      <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:grid-cols-[minmax(0,1fr)_clamp(18.125rem,24%,22rem)] lg:grid-rows-[auto_1fr] lg:gap-x-5">
        <Card className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 p-3 lg:col-start-2 lg:row-start-1 lg:grid-cols-1">
          {guest ? (
            <Button
              variant="cta"
              size="md"
              className={SOLVE_BUTTON_CLASS}
              onClick={() => prompt.ask(`/problems/${problemId}`)}
            >
              <Code2 className="mr-2 h-4 w-4" />
              {t("btnSolve")}
            </Button>
          ) : (
            <Button
              asChild
              variant="cta"
              size="md"
              className={SOLVE_BUTTON_CLASS}
            >
              <Link href={`/workspace/${problemId}`}>
                <Code2 className="mr-2 h-4 w-4" />
                {t("btnSolve")}
              </Link>
            </Button>
          )}
          <Button
            variant="ghost"
            size="md"
            aria-pressed={saved}
            className={`w-full border ${
              saved
                ? "border-[var(--color-admin-warn)] bg-[color-mix(in_srgb,var(--color-admin-warn)_18%,transparent)] text-[var(--color-admin-warn-text)]"
                : "border-[var(--color-border)]"
            }`}
            disabled={setBookmark.isPending}
            onClick={
              guest ? () => prompt.ask(`/problems/${problemId}`) : toggleSaved
            }
          >
            <Bookmark
              className={`mr-2 h-4 w-4 ${saved ? "fill-current" : ""}`}
            />
            {saved ? td("panel.btnUnsave") : td("panel.btnSave")}
          </Button>
        </Card>

        <Card className="min-w-0 px-4 py-5 sm:px-7 lg:col-start-1 lg:row-start-1 lg:row-span-2">
          <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2 border-b border-[var(--color-border)] pb-3">
            <h2 className="text-xl font-bold">{t("statementTitle")}</h2>
            {detail.examples.length > 0 ? (
              <a
                href="#problem-examples"
                className="rounded-sm text-sm font-semibold text-[var(--color-accent-blue)] hover:underline focus-visible:outline-2 focus-visible:outline-offset-2"
              >
                {t("jumpToExamples")}
              </a>
            ) : null}
          </div>
          <MarkdownPreview
            size="lg"
            className="[&>*+*]:mt-2.5 [&_.katex-display]:overflow-x-auto [&_table]:block [&_table]:overflow-x-auto"
          >
            {detail.statementMd}
          </MarkdownPreview>

          {detail.examples.length > 0 ? (
            <section
              id="problem-examples"
              aria-labelledby="problem-examples-title"
              tabIndex={-1}
              className="mt-6 scroll-mt-24 border-t border-[var(--color-border)] pt-4"
            >
              <h2 id="problem-examples-title" className="text-xl font-bold">
                {td("panel.examplesTitle")}
              </h2>
              <ul className="mt-3 space-y-5">
                {detail.examples.map((example, index) => {
                  const no = index + 1;
                  return (
                    <li key={index}>
                      <h3 className="mb-2 text-sm font-bold">
                        {t("exampleTitle", { no })}
                      </h3>
                      <div className="grid gap-2.5 sm:grid-cols-2">
                        <ExampleBlock
                          label={t("exampleInput")}
                          text={example.input}
                          copyLabel={t("btnCopy")}
                          copyName={t("copyExampleInput", { no })}
                          onCopy={copy}
                        />
                        <ExampleBlock
                          label={t("exampleOutput")}
                          text={example.output}
                          copyLabel={t("btnCopy")}
                          copyName={t("copyExampleOutput", { no })}
                          onCopy={copy}
                        />
                      </div>
                      {example.explanation ? (
                        <p className="mt-2 whitespace-pre-wrap break-words text-sm text-[var(--color-text-muted)]">
                          {example.explanation}
                        </p>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            </section>
          ) : null}
        </Card>

        <aside className="flex min-w-0 flex-col gap-3 lg:col-start-2 lg:row-start-2">
          <Card className="p-4">
            <h2 className="text-sm font-bold">{t("languagesTitle")}</h2>
            {/* No limits (guest, or the judge read failed): the language names alone. */}
            {LANGUAGES.some((item) => !detail.limitsByLanguage[item]) ? (
              <p className="mt-1 text-sm text-[var(--color-text-muted)]">
                {LANGUAGES.map((language) => td(`language.${language}`)).join(
                  ", ",
                )}
              </p>
            ) : (
              <table className="mt-2 w-full text-[13px]">
                <caption className="sr-only">{t("limitsCaption")}</caption>
                <thead>
                  <tr className="text-left text-[12px] text-[var(--color-text-muted)]">
                    <th scope="col" className="pb-1 font-semibold">
                      {t("colLanguage")}
                    </th>
                    <th scope="col" className="pb-1 text-right font-semibold">
                      {t("colTime")}
                    </th>
                    <th scope="col" className="pb-1 text-right font-semibold">
                      {t("colMemory")}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {LANGUAGES.map((language) => (
                    <tr key={language}>
                      <td className="py-0.5">{td(`language.${language}`)}</td>
                      <td className="py-0.5 text-right font-mono">
                        {detail.limitsByLanguage[language]?.timeLimitMs} ms
                      </td>
                      <td className="py-0.5 text-right font-mono">
                        {detail.limitsByLanguage[language]?.memoryLimitMb} MB
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <h2 className="mb-2 mt-4 border-t border-[var(--color-border)] pt-3 text-sm font-bold">
              {t("infoTitle")}
            </h2>
            <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 text-[13px]">
              <dt className="text-[var(--color-text-muted)]">
                {t("acceptanceRate")}
              </dt>
              <dd className="text-right font-mono font-semibold">
                {detail.acRate === null ? "-" : `${detail.acRate}%`}
              </dd>
              <dt className="text-[var(--color-text-muted)]">
                {t("hiddenTestcases")}
              </dt>
              <dd className="text-right font-mono font-semibold">
                {detail.hiddenTestcaseCount}
              </dd>
            </dl>
          </Card>

          {guest || detail.mySubmissions === null ? null : (
            <Card className="px-4 py-4">
              <h2 className="mb-2 text-sm font-bold">{t("recentTitle")}</h2>
              {recent.length === 0 ? (
                <p className="text-[13px] text-[var(--color-text-muted)]">
                  {td("panel.noSubmissions")}
                </p>
              ) : (
                <ul className="flex flex-col gap-1.5">
                  {recent.map((item) => (
                    <li key={item.id}>
                      <Link
                        href={`/submissions/${item.id}`}
                        className="flex items-center justify-between gap-2 rounded-md px-1 py-0.5 text-[13px] hover:bg-[var(--color-row-hover)]"
                      >
                        <span className="font-semibold">
                          {td(`verdict.${item.verdict}`)}
                        </span>
                        <span className="text-[var(--color-text-muted)]">
                          {td(`language.${item.language}`)} ·{" "}
                          {formatDateVN(item.submittedAt, {
                            locale,
                            withTime: true,
                          })}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
              <p className="mt-2 text-[13px]">
                <Link href="/submissions" className="underline">
                  {t("linkAllSubmissions")}
                </Link>
              </p>
            </Card>
          )}

          {!guest && solutionReview ? (
            <Card className="px-4 py-4">
              <h2 className="mb-2 text-sm font-bold">
                {td("panel.tabReview")}
              </h2>
              <dl className="grid grid-cols-[1fr_auto] gap-x-3 gap-y-1 text-[13px]">
                <dt className="text-[var(--color-text-muted)]">
                  {td("panel.timeComplexity")}
                </dt>
                <dd className="font-mono font-semibold">
                  {solutionReview.timeComplexity}
                </dd>
                <dt className="text-[var(--color-text-muted)]">
                  {td("panel.spaceComplexity")}
                </dt>
                <dd className="font-mono font-semibold">
                  {solutionReview.spaceComplexity}
                </dd>
              </dl>
              {reviewSubmissionId ? (
                <p className="mt-2 text-[13px]">
                  <Link
                    href={`/submissions/${reviewSubmissionId}/review`}
                    className="underline"
                  >
                    {td("panel.linkFullReport")}
                  </Link>
                </p>
              ) : null}
            </Card>
          ) : null}
        </aside>
      </div>
      <LoginPromptDialog
        open={prompt.open}
        returnTo={prompt.returnTo}
        onClose={prompt.close}
      />
    </div>
  );
}

// One labelled sample text with a Copy button pinned to its top-right corner.
function ExampleBlock({
  label,
  text,
  copyLabel,
  copyName,
  onCopy,
}: {
  label: string;
  text: string;
  copyLabel: string;
  copyName: string;
  onCopy: (text: string) => void;
}) {
  return (
    <div className="min-w-0 overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-text)] text-[var(--color-surface)] [.dark_&]:bg-[var(--color-background)] [.dark_&]:text-[var(--color-text)]">
      <div className="flex flex-wrap items-center justify-between gap-1 border-b border-[color-mix(in_srgb,currentColor_22%,transparent)] bg-[color-mix(in_srgb,var(--color-primary)_14%,transparent)] px-3 py-1">
        <span className="text-sm font-semibold">{label}</span>
        <button
          type="button"
          aria-label={copyName}
          onClick={() => onCopy(text)}
          className="min-h-8 rounded-md border border-[color-mix(in_srgb,currentColor_36%,transparent)] px-2.5 text-xs font-semibold hover:bg-[color-mix(in_srgb,currentColor_14%,transparent)] focus-visible:outline-2 focus-visible:outline-offset-1"
        >
          {copyLabel}
        </button>
      </div>
      <pre className="overflow-x-auto px-3 py-3 font-mono text-sm">{text}</pre>
    </div>
  );
}
