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
// Topics and difficulty levels are admin-managed lists (DEC-2026-1001-admin-configurable-settings). A2 and
// A3 both edit the entire bank (DEC-2026-0830-interview-bank-crud), with no "only what I authored" rule.
"use client";

import { useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Copy, Gauge, Pencil, Tags, Trash2 } from "lucide-react";
import {
  fetchInterviewQuestionPage,
  levelLabel,
  levelTone,
  topicLabel,
  useInterviewLevels,
  useInterviewTopics,
  type InterviewQuestion,
  type QuestionLevel,
  type QuestionTopic,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { usePersistedPageSize } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  BulkActionBar,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  IconAction,
  PageHeader,
  Pagination,
  FilterBar,
  FilterMenu,
  type DataTableColumn,
} from "@/shared/ui";
import { LevelManagerDialog } from "./level-manager-dialog";
import { TopicManagerDialog } from "./topic-manager-dialog";

// 8 is the default, 20 and 50 the other choices: same as the problem list (SHR0201 Q4).
const PAGE_SIZES = [8, 20, 50] as const;


type SortKey = "code" | "question" | "topic" | "level" | "followUps" | "rubric" | "usage" | "score";

type TopicFilter = QuestionTopic | "all";
type LevelFilter = QuestionLevel | "all";

type Props = {
  /** Area root for row links and the create button, e.g. "/admin/interview-questions". */
  basePath: string;
  /** Topic management is ADMIN-only; the screen is shared with INSTRUCTOR, who may only pick topics. */
  canManageTopics?: boolean;
};

export function InterviewQuestionManagementView({ basePath, canManageTopics = false }: Props) {
  const t = useT("interviewQuestionManagement");
  const [page] = useState(fetchInterviewQuestionPage);
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const [managingTopics, setManagingTopics] = useState(false);
  const [managingLevels, setManagingLevels] = useState(false);
  const [query, setQuery] = useState("");
  const [topic, setTopic] = useState<TopicFilter>("all");
  const [level, setLevel] = useState<LevelFilter>("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = usePersistedPageSize("algoprep-interview-questions-page-size", PAGE_SIZES, 8);
  // Empty key = no column sorted yet, so the bank keeps its natural order until a header is clicked.
  const [sort, setSort] = useState<{ key: SortKey | ""; direction: "asc" | "desc" }>({
    key: "",
    direction: "asc",
  });
  const csvInput = useRef<HTMLInputElement>(null);
  const [pendingDelete, setPendingDelete] = useState<InterviewQuestion | null>(null);
  const [deletedCodes, setDeletedCodes] = useState<ReadonlySet<string>>(new Set());
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [confirmingBulkDelete, setConfirmingBulkDelete] = useState(false);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = page.questions.filter(
      (question) =>
        !deletedCodes.has(question.code) &&
        (topic === "all" || question.topic === topic) &&
        (level === "all" || question.level === level) &&
        (!needle ||
          question.question.toLowerCase().includes(needle) ||
          question.code.toLowerCase().includes(needle)),
    );

    if (!sort.key) return rows;
    const factor = sort.direction === "asc" ? 1 : -1;
    const levelRank = (key: string) => levelList.findIndex((item) => item.key === key);
    return [...rows].sort((left, right) => {
      switch (sort.key) {
        case "code":
          return left.code.localeCompare(right.code, "vi", { numeric: true }) * factor;
        case "question":
          return left.question.localeCompare(right.question, "vi") * factor;
        case "topic":
          return topicLabel(topicList, left.topic).localeCompare(topicLabel(topicList, right.topic), "vi") * factor;
        case "level":
          // Admin-chosen order of the level list, not a fixed easy < medium < hard.
          return (levelRank(left.level) - levelRank(right.level)) * factor;
        case "followUps":
          return (left.followUps.length - right.followUps.length) * factor;
        case "rubric":
          return (left.rubric.length - right.rubric.length) * factor;
        case "usage":
          return (left.usageCount - right.usageCount) * factor;
        default:
          return (left.averageScore - right.averageScore) * factor;
      }
    });
  }, [page.questions, deletedCodes, query, topic, level, sort, topicList, levelList]);

  // Questions still on screen (not locally deleted) per topic, so the manager can refuse deleting a
  // topic that is in use.
  const topicUsage = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const question of page.questions) {
      if (!deletedCodes.has(question.code)) counts[question.topic] = (counts[question.topic] ?? 0) + 1;
    }
    return counts;
  }, [page.questions, deletedCodes]);

  const levelUsage = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const question of page.questions) {
      if (!deletedCodes.has(question.code)) counts[question.level] = (counts[question.level] ?? 0) + 1;
    }
    return counts;
  }, [page.questions, deletedCodes]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  // Deleting rows can leave the stored page past the last one; clamp rather than render an empty page.
  const safePage = Math.min(currentPage, totalPages);
  const visible = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  // Selection survives filter changes, so a bulk delete can reach rows the filter now hides.
  const hiddenSelectedCount = useMemo(() => {
    const shown = new Set(filtered.map((question) => question.code));
    return [...selected].filter((code) => !shown.has(code)).length;
  }, [filtered, selected]);

  function toggleRow(code: string) {
    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
  }

  function deleteSelected() {
    const count = selected.size;
    setDeletedCodes((previous) => new Set([...previous, ...selected]));
    setSelected(new Set());
    toast.success(t("toast.bulkDeleted", { count }));
  }

  // Live filtering stays silent; only an explicit submit (Enter) reports the result count.
  function announceSearch() {
    toast.info(
      filtered.length === 0
        ? t("toast.searchEmpty")
        : t("toast.searchResult", { count: filtered.length }),
    );
  }

  function resetToFirstPage<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value);
      setCurrentPage(1);
    };
  }

  const columns: DataTableColumn<InterviewQuestion>[] = [
    {
      key: "code",
      sortable: true,
      header: t("columnCode"),
      width: "92px",
      render: (question) => (
        <span className="font-mono text-xs whitespace-nowrap text-[var(--color-text-muted)]">{question.code}</span>
      ),
    },
    {
      key: "question",
      sortable: true,
      header: t("columnQuestion"),
      width: "38%",
      render: (question) => (
        <Link
          href={`${basePath}/${question.code}`}
          title={question.question}
          className="block w-0 min-w-full truncate font-semibold hover:underline"
        >
          {question.question}
        </Link>
      ),
    },
    {
      key: "topic",
      sortable: true,
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
      sortable: true,
      header: t("columnLevel"),
      width: "116px",
      render: (question) => (
        <Badge variant={levelTone(levelList, question.level)}>
          {levelLabel(levelList, question.level)}
        </Badge>
      ),
    },
    {
      key: "followUps",
      sortable: true,
      header: t("columnFollowUps"),
      width: "84px",
      align: "right",
      render: (question) => <span className="font-mono">{question.followUps.length}</span>,
    },
    {
      key: "rubric",
      sortable: true,
      header: t("columnRubric"),
      width: "84px",
      align: "right",
      render: (question) => <span className="font-mono">{question.rubric.length}</span>,
    },
    {
      key: "usage",
      sortable: true,
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
      sortable: true,
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
            href={`${basePath}/${question.code}/edit`}
          />
          <IconAction
            icon={Copy}
            label={t("duplicate")}
            ariaLabel={t("duplicateQuestion", { code: question.code })}
            onClick={() => toast.info(t("toast.notWired", { action: t("duplicate") }))}
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
                onClick={() => setManagingLevels(true)}
              >
                <Gauge aria-hidden="true" className="h-4 w-4" />
                {t("manageLevels")}
              </Button>
            ) : null}
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
            <input
              ref={csvInput}
              type="file"
              accept=".csv,text/csv"
              hidden
              data-testid="csv-input"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) toast.info(t("toast.notWired", { action: t("importCsv") }));
                event.target.value = "";
              }}
            />
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => csvInput.current?.click()}
            >
              {t("importCsv")}
            </Button>
            <Button asChild variant="cta" size="sm">
              <Link href={`${basePath}/new`}>{t("newQuestion")}</Link>
            </Button>
          </>
        }
      />

      <Card className="min-w-0 px-[18px] py-4">
        <FilterBar
          search={{
            label: t("searchLabel"),
            placeholder: t("searchPlaceholder"),
            value: query,
            onChange: (next) => {
              setQuery(next);
              setCurrentPage(1);
            },
            onSubmit: announceSearch,
          }}
          resultCount={t("resultCount", { shown: filtered.length, total: page.questions.length })}
        >
          <FilterMenu
            label={t("topicFilterLabel")}
            value={topic}
            onValueChange={resetToFirstPage(setTopic)}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...topicList.map((topic) => ({ value: topic.key, label: topic.label })),
            ]}
          />
          <FilterMenu
            label={t("levelFilterLabel")}
            value={level}
            onValueChange={resetToFirstPage(setLevel)}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...levelList.map((item) => ({ value: item.key, label: item.label })),
            ]}
          />
        </FilterBar>

        <BulkActionBar count={selected.size} label={t("selectionLabel", { count: selected.size })}>
          {/* ponytail: no API behind duplicate yet, so it says so, same as the per-row action. */}
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={() => toast.info(t("toast.notWired", { action: t("duplicate") }))}
          >
            {t("bulk.duplicate")}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-[var(--color-admin-negative)]"
            onClick={() => setConfirmingBulkDelete(true)}
          >
            {t("bulk.delete")}
          </Button>
        </BulkActionBar>

        <DataTable
          caption={t("tableCaption")}
          columns={columns}
          rows={visible}
          rowKey={(question) => question.code}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: (selectAll) =>
              setSelected(selectAll ? new Set(visible.map((row) => row.code)) : new Set()),
            selectAllLabel: t("selectAll"),
            rowLabel: (question) => t("selectRow", { code: question.code }),
          }}
          emptyMessage={t("emptyFiltered")}
          minWidth={1000}
          sort={{
            key: sort.key,
            direction: sort.direction,
            onSortChange: (key, direction) => {
              setSort({ key: key as SortKey, direction });
              setCurrentPage(1);
            },
          }}
        />

        <Pagination
          page={safePage}
          pageSize={pageSize}
          pageSizeOptions={PAGE_SIZES}
          pageSizeLabel={t("pageSizeLabel")}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setCurrentPage(1);
          }}
          total={filtered.length}
          onPageChange={setCurrentPage}
          summary={t("pageLabel", {
            page: safePage,
            totalPages,
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

      <LevelManagerDialog
        open={managingLevels}
        onClose={() => setManagingLevels(false)}
        usage={levelUsage}
      />

      <ConfirmDialog
        open={confirmingBulkDelete}
        onClose={() => setConfirmingBulkDelete(false)}
        onConfirm={() => {
          deleteSelected();
          setConfirmingBulkDelete(false);
        }}
        title={t("confirmBulkDeleteTitle", { count: selected.size })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
        {hiddenSelectedCount > 0 ? (
          <span className="mt-2 block font-semibold text-[var(--color-admin-warn-text)]">
            {t("confirmBulkDeleteHidden", { count: hiddenSelectedCount })}
          </span>
        ) : null}
      </ConfirmDialog>

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) {
            setDeletedCodes((previous) => new Set(previous).add(pendingDelete.code));
            // A deleted row must not stay counted in the selection.
            setSelected((previous) => {
              const next = new Set(previous);
              next.delete(pendingDelete.code);
              return next;
            });
            toast.success(t("toast.deleted", { code: pendingDelete.code }));
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
