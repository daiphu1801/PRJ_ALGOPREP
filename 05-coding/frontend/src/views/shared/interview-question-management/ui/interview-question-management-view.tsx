// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html: header with a CSV import and a
// "Câu hỏi mới" CTA (:147-149), stat cards (:151-163), a filter bar (:165-183), then a card grid
// (:185-233) and numbered paging (:235-247).
//
// Card grid, not a table: each question carries follow-ups and a weighted rubric, which a row
// cannot hold without truncating the thing the screen exists to show. The card lives in
// entities/interview-question so the student-facing bank screen can reuse it.
//
// FIVE topics, per DEC-2026-0830-interview-bank-crud — the taxonomy that replaced a stale set of
// four. A2 and A3 both edit the entire bank, with no "only what I authored" restriction.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import {
  QUESTION_LEVELS,
  QUESTION_TOPICS,
  fetchInterviewQuestionPage,
  InterviewQuestionCard,
  type InterviewQuestion,
  type QuestionLevel,
  type QuestionTopic,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import {
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  PageHeader,
  Pagination,
  SegmentedTabs,
  StatCard,
  TextField,
} from "@/shared/ui";

const PAGE_SIZE = 6;

type TopicFilter = QuestionTopic | "all";
type LevelFilter = QuestionLevel | "all";

export function InterviewQuestionManagementView() {
  const t = useT("interviewQuestionManagement");
  const [page] = useState(fetchInterviewQuestionPage);
  const [query, setQuery] = useState("");
  const [topic, setTopic] = useState<TopicFilter>("all");
  const [level, setLevel] = useState<LevelFilter>("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingDelete, setPendingDelete] = useState<InterviewQuestion | null>(null);
  const [deletedCodes, setDeletedCodes] = useState<ReadonlySet<string>>(new Set());

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return page.questions.filter(
      (question) =>
        !deletedCodes.has(question.code) &&
        (topic === "all" || question.topic === topic) &&
        (level === "all" || question.level === level) &&
        (!needle ||
          question.question.toLowerCase().includes(needle) ||
          question.code.toLowerCase().includes(needle)),
    );
  }, [page.questions, deletedCodes, query, topic, level]);

  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function resetToFirstPage<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value);
      setCurrentPage(1);
    };
  }

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle", { total: page.totalQuestions })}
        actions={
          <>
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {t("importCsv")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href="/admin/interview-questions/new">{t("newQuestion")}</Link>
            </Button>
          </>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        {page.stats.map((stat) => (
          <StatCard
            key={stat.key}
            label={t(`stat.${stat.key}.label`)}
            value={stat.value}
            delta={
              stat.delta ? (
                <span style={{ color: `var(${stat.deltaColorVar})` }}>{stat.delta}</span>
              ) : undefined
            }
            meta={t(`stat.${stat.key}.meta`)}
          />
        ))}
      </div>

      <Card className="mb-4 px-4 py-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("searchLabel")}
            hideLabel
            leadingIcon={<Search className="h-3.5 w-3.5" />}
            placeholder={t("searchPlaceholder")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setCurrentPage(1);
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("topicFilterLabel")}
            value={topic}
            onValueChange={resetToFirstPage(setTopic)}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...QUESTION_TOPICS.map((key) => ({ value: key, label: t(`topic.${key}`) })),
            ]}
          />
          <SegmentedTabs
            label={t("levelFilterLabel")}
            value={level}
            onValueChange={resetToFirstPage(setLevel)}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...QUESTION_LEVELS.map((key) => ({ value: key, label: t(`level.${key}`) })),
            ]}
          />
          <span className="ml-auto text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
            {t("resultCount", { shown: filtered.length, total: page.questions.length })}
          </span>
        </div>
      </Card>

      {visible.length === 0 ? (
        <Card>
          <EmptyState>{t("emptyFiltered")}</EmptyState>
        </Card>
      ) : (
        <div className="grid items-start gap-3.5 [grid-template-columns:repeat(auto-fill,minmax(340px,1fr))]">
          {visible.map((question) => (
            <InterviewQuestionCard
              key={question.code}
              question={question}
              topicLabel={t(`topic.${question.topic}`)}
              levelLabel={t(`level.${question.level}`)}
              followUpsLabel={t("followUps")}
              rubricLabel={t("rubric")}
              usageLabel={t("usage", {
                count: question.usageCount,
                score: question.averageScore.toFixed(1),
              })}
              actions={
                <>
                  <Button
                    asChild
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                  >
                    <Link href={`/admin/interview-questions/${question.code}`}>{t("edit")}</Link>
                  </Button>
                  <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
                    {t("duplicate")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={t("deleteQuestion", { code: question.code })}
                    onClick={() => setPendingDelete(question)}
                    className="border border-[var(--color-border)] px-2 text-[var(--color-admin-negative)]"
                  >
                    {t("delete")}
                  </Button>
                </>
              }
            />
          ))}
        </div>
      )}

      <Card className="mt-4 px-4 py-2">
        <Pagination
          page={currentPage}
          pageSize={PAGE_SIZE}
          total={filtered.length}
          onPageChange={setCurrentPage}
          summary={t("pageLabel", {
            page: currentPage,
            totalPages: Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)),
            shown: visible.length,
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
          showPageNumbers
          pageLabel={(value) => t("goToPage", { page: value })}
          className="pt-0"
        />
      </Card>

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) {
            setDeletedCodes((previous) => new Set(previous).add(pendingDelete.code));
          }
          setPendingDelete(null);
        }}
        title={t("confirmDeleteTitle", { code: pendingDelete?.code ?? "" })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
      </ConfirmDialog>
    </div>
  );
}
