// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Trang chi tiết một bài toán (/problems/[problemId]): đọc đề, giới hạn, bài nộp gần đây, rồi mới bấm
// "Giải bài toán" để vào Workspace (/workspace/[problemId]). Tách ra khỏi Workspace theo yêu cầu owner
// 2026-10-20; bố cục theo trang đề bài kiểu online judge (owner gửi ảnh mẫu 2026-10-20): tiêu đề có
// đường kẻ, cột trái là đề và ví dụ có nút Copy, cột phải là nút hành động lớn cùng thông tin bài.
// Nhãn dùng chung namespace `problemDetail`, chỉ nhãn riêng của trang này nằm ở `problemOverview`.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Bookmark, Code2 } from "lucide-react";
import {
  fetchProblemDetail,
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  type SubmissionLanguage,
} from "@/entities/problem";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { Badge, Button, Card, EmptyState, MarkdownPreview } from "@/shared/ui";

const LANGUAGES: SubmissionLanguage[] = ["java", "cpp", "python"];
const RECENT_SUBMISSION_LIMIT = 3;

export function ProblemOverviewView() {
  const params = useParams<{ problemId?: string }>();
  const problemId = params?.problemId;
  const detail = useMemo(
    () => (problemId ? fetchProblemDetail(problemId) : undefined),
    [problemId],
  );
  const t = useT("problemOverview");
  const td = useT("problemDetail");
  const locale = useLocale() === "en" ? "en" : "vi";
  const router = useRouter();
  const levels = useProblemLevels();
  const [saved, setSaved] = useState(detail?.saved ?? false);

  // Unknown id reads as "not found", never as "no permission" (USR0102 BD Sheet 9 NO 2).
  if (!problemId || !detail) {
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

  function toggleSaved() {
    if (saved) {
      // Undo restores the bookmark (owner 2026-10-20, same choice as USR0103: no confirm popup).
      toast.info(td("toast.unsaved"), {
        label: t("btnUndo"),
        onClick: () => setSaved(true),
      });
    } else {
      toast.success(td("toast.saved"), {
        label: td("toast.openSaved"),
        onClick: () => router.push("/saved"),
      });
    }
    setSaved(!saved);
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
  const sorted = [...detail.mySubmissions].sort((a, b) =>
    b.submittedAt.localeCompare(a.submittedAt),
  );
  const recent = sorted.slice(0, RECENT_SUBMISSION_LIMIT);
  const reviewSubmission = sorted.find((item) => item.verdict === "accepted");
  const sectionTitle = "mb-3 text-xl font-bold tracking-tight";

  return (
    <div className="p-6">
      <header className="mb-6 flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-[var(--color-border)] pb-4">
        <h1 className="min-w-0 text-3xl font-bold tracking-tight">
          {detail.title}
        </h1>
        <span className="font-mono text-sm text-[var(--color-text-muted)]">
          {detail.code}
        </span>
        <Badge
          variant={problemLevelTone(levels, detail.difficulty)}
          className="px-3 py-1 text-[13px]"
        >
          {problemLevelLabel(levels, detail.difficulty)}
        </Badge>
        <Button
          asChild
          variant="ghost"
          size="md"
          className="ml-auto border border-[var(--color-border)]"
        >
          <Link href="/problems">
            <ArrowLeft className="mr-2 h-4 w-4" />
            {t("btnBack")}
          </Link>
        </Button>
      </header>

      <div className="grid grid-cols-1 gap-6 lg:[grid-template-columns:minmax(0,1fr)_300px]">
        <Card className="min-w-0 px-7 py-6 lg:min-h-[60dvh]">
          {/* Constraints and the follow-up question are sections of this Markdown, not fields. */}
          <MarkdownPreview size="lg">{detail.statementMd}</MarkdownPreview>

          <hr className="my-6 border-[var(--color-border)]" />
          <h2 className={sectionTitle}>{td("panel.examplesTitle")}</h2>
          <ul className="flex flex-col gap-5">
            {detail.examples.map((example, index) => {
              const no = String(index + 1).padStart(2, "0");
              return (
                <li key={index} className="flex flex-col gap-3">
                  <ExampleBlock
                    label={t("exampleInput", { no })}
                    text={example.input}
                    copyLabel={t("btnCopy")}
                    onCopy={copy}
                  />
                  <ExampleBlock
                    label={t("exampleOutput", { no })}
                    text={example.output}
                    copyLabel={t("btnCopy")}
                    onCopy={copy}
                  />
                  {example.explanation ? (
                    <p className="text-[13.5px] text-[var(--color-text-muted)]">
                      {example.explanation}
                    </p>
                  ) : null}
                </li>
              );
            })}
          </ul>
        </Card>

        <aside className="flex flex-col gap-5 lg:sticky lg:top-24 lg:self-start">
          <Button asChild variant="cta" size="md" className="w-full">
            <Link href={`/workspace/${problemId}`}>
              <Code2 className="mr-2 h-4 w-4" />
              {t("btnSolve")}
            </Link>
          </Button>
          <Button
            variant="ghost"
            size="md"
            aria-pressed={saved}
            className={`w-full border ${
              saved
                ? "border-[var(--color-admin-warn)] bg-[color-mix(in_srgb,var(--color-admin-warn)_18%,transparent)] text-[var(--color-admin-warn-text)]"
                : "border-[var(--color-border)]"
            }`}
            onClick={toggleSaved}
          >
            <Bookmark className={`mr-2 h-4 w-4 ${saved ? "fill-current" : ""}`} />
            {saved ? td("panel.btnUnsave") : td("panel.btnSave")}
          </Button>

          <Card className="px-4 py-4">
            <h2 className="mb-2 text-sm font-bold">{t("infoTitle")}</h2>
            <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 text-[13px]">
              <dt className="text-[var(--color-text-muted)]">
                {td("panel.acRate")}
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
            <table className="mt-3 w-full text-[13px]">
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
                      {detail.limitsByLanguage[language].timeLimitMs} ms
                    </td>
                    <td className="py-0.5 text-right font-mono">
                      {detail.limitsByLanguage[language].memoryLimitMb} MB
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          <Card className="px-4 py-4">
            <h2 className="mb-2 text-sm font-bold">{t("topicsTitle")}</h2>
            <div className="flex flex-wrap gap-2">
              {detail.topics.map((topic) => (
                <Badge key={topic} variant="neutral">
                  {topic}
                </Badge>
              ))}
            </div>
            <h2 className="mb-1 mt-4 text-sm font-bold">
              {t("languagesTitle")}
            </h2>
            <p className="text-[13px] text-[var(--color-text-muted)]">
              {LANGUAGES.map((language) => td(`language.${language}`)).join(
                ", ",
              )}
            </p>
          </Card>

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

          {detail.solutionReview ? (
            <Card className="px-4 py-4">
              <h2 className="mb-2 text-sm font-bold">
                {td("panel.tabReview")}
              </h2>
              <dl className="grid grid-cols-[1fr_auto] gap-x-3 gap-y-1 text-[13px]">
                <dt className="text-[var(--color-text-muted)]">
                  {td("panel.timeComplexity")}
                </dt>
                <dd className="font-mono font-semibold">
                  {detail.solutionReview.timeComplexity}
                </dd>
                <dt className="text-[var(--color-text-muted)]">
                  {td("panel.spaceComplexity")}
                </dt>
                <dd className="font-mono font-semibold">
                  {detail.solutionReview.spaceComplexity}
                </dd>
              </dl>
              {reviewSubmission ? (
                <p className="mt-2 text-[13px]">
                  <Link
                    href={`/submissions/${reviewSubmission.id}/review`}
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
    </div>
  );
}

// One labelled sample text with a Copy button pinned to its top-right corner.
function ExampleBlock({
  label,
  text,
  copyLabel,
  onCopy,
}: {
  label: string;
  text: string;
  copyLabel: string;
  onCopy: (text: string) => void;
}) {
  return (
    <div>
      <h3 className="mb-1.5 text-base font-bold">{label}</h3>
      <div className="relative">
        <pre className="overflow-x-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-hover)] px-4 py-4 pr-20 font-mono text-[13.5px]">
          {text}
        </pre>
        <button
          type="button"
          onClick={() => onCopy(text)}
          className="absolute right-0 top-0 rounded-bl-lg rounded-tr-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-xs font-semibold text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
        >
          {copyLabel}
        </button>
      </div>
    </div>
  );
}
