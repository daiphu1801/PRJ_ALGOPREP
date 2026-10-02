// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html: header with a CSV import and a
// "Câu hỏi mới" CTA (:147-149), a filter bar (:165-183), then the list and numbered paging
// (:235-247).
//
// List, not cards: same DataTable layout as problem-management so the two admin bank screens read
// alike (owner instruction 2026-10-01). Follow-ups and rubric show as counts; their full text lives
// on the edit screen. InterviewQuestionCard stays in entities/interview-question for the
// student-facing bank screen.
//
// FIVE topics, per DEC-2026-0830-interview-bank-crud — the taxonomy that replaced a stale set of
// four. A2 and A3 both edit the entire bank, with no "only what I authored" restriction.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Copy, Pencil, Search, Tags, Trash2 } from "lucide-react";
import {
  QUESTION_LEVELS,
  fetchInterviewQuestionPage,
  topicLabel,
  useInterviewTopics,
  type InterviewQuestion,
  type QuestionLevel,
  type QuestionTopic,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  IconAction,
  PageHeader,
  Pagination,
  SegmentedTabs,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";
import { TopicManagerDialog } from "./topic-manager-dialog";

const LEVEL_VARIANT: Record<QuestionLevel, BadgeVariant> = {
  easy: "success",
  medium: "warn",
  hard: "negative",
};

const PAGE_SIZE = 8;

type TopicFilter = QuestionTopic | "all";
type LevelFilter = QuestionLevel | "all";

type Props = {
  /** Topic management is ADMIN-only; the screen is shared with INSTRUCTOR, who may only pick topics. */
  canManageTopics?: boolean;
};

export function InterviewQuestionManagementView({ canManageTopics = false }: Props) {
  const t = useT("interviewQuestionManagement");
  const [page] = useState(fetchInterviewQuestionPage);
  const topicList = useInterviewTopics();
  const [managingTopics, setManagingTopics] = useState(false);
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

  // Questions still on screen (not locally deleted) per topic, so the manager can refuse deleting a
  // topic that is in use.
  const topicUsage = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const question of page.questions) {
      if (!deletedCodes.has(question.code)) counts[question.topic] = (counts[question.topic] ?? 0) + 1;
    }
    return counts;
  }, [page.questions, deletedCodes]);

  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function resetToFirstPage<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value);
      setCurrentPage(1);
    };
  }

  const columns: DataTableColumn<InterviewQuestion>[] = [
    {
      key: "code",
      header: t("columnCode"),
      width: "92px",
      render: (question) => (
        <span className="font-mono text-xs text-[var(--color-text-muted)]">{question.code}</span>
      ),
    },
    {
      key: "question",
      header: t("columnQuestion"),
      render: (question) => (
        <Link
          href={`/admin/interview-questions/${question.code}`}
          className="block truncate font-semibold hover:underline"
        >
          {question.question}
        </Link>
      ),
    },
    {
      key: "topic",
      header: t("columnTopic"),
      width: "140px",
      render: (question) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {topicLabel(topicList, question.topic)}
        </span>
      ),
    },
    {
      key: "level",
      header: t("columnLevel"),
      width: "116px",
      render: (question) => (
        <Badge variant={LEVEL_VARIANT[question.level]}>{t(`level.${question.level}`)}</Badge>
      ),
    },
    {
      key: "followUps",
      header: t("columnFollowUps"),
      width: "84px",
      align: "right",
      render: (question) => <span className="font-mono">{question.followUps.length}</span>,
    },
    {
      key: "rubric",
      header: t("columnRubric"),
      width: "84px",
      align: "right",
      render: (question) => <span className="font-mono">{question.rubric.length}</span>,
    },
    {
      key: "usage",
      header: t("columnUsage"),
      width: "96px",
      align: "right",
      render: (question) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {question.usageCount.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "score",
      header: t("columnScore"),
      width: "76px",
      align: "right",
      render: (question) => (
        <span className="font-mono font-semibold">{question.averageScore.toFixed(1)}</span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "128px",
      align: "right",
      render: (question) => (
        <span className="flex justify-end gap-1">
          <IconAction
            icon={Pencil}
            label={t("edit")}
            ariaLabel={t("editQuestion", { code: question.code })}
            href={`/admin/interview-questions/${question.code}`}
          />
          <IconAction
            icon={Copy}
            label={t("duplicate")}
            ariaLabel={t("duplicateQuestion", { code: question.code })}
          />
          <IconAction
            icon={Trash2}
            label={t("delete")}
            ariaLabel={t("deleteQuestion", { code: question.code })}
            tone="danger"
            onClick={() => setPendingDelete(question)}
          />
        </span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle", { total: page.totalQuestions })}
        actions={
          <>
            {canManageTopics ? (
              <Button
                variant="ghost"
                size="sm"
                className="gap-1.5 border border-[var(--color-border)]"
                onClick={() => setManagingTopics(true)}
              >
                <Tags aria-hidden="true" className="h-4 w-4" />
                {t("manageTopics")}
              </Button>
            ) : null}
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {t("importCsv")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href="/admin/interview-questions/new">{t("newQuestion")}</Link>
            </Button>
          </>
        }
      />

      <Card className="min-w-0 px-[18px] py-4">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
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
              ...topicList.map((topic) => ({ value: topic.key, label: topic.label })),
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

        <DataTable
          caption={t("tableCaption")}
          columns={columns}
          rows={visible}
          rowKey={(question) => question.code}
          emptyMessage={t("emptyFiltered")}
          minWidth={1000}
        />

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
        />
      </Card>

      <TopicManagerDialog
        open={managingTopics}
        onClose={() => setManagingTopics(false)}
        usage={topicUsage}
      />

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
