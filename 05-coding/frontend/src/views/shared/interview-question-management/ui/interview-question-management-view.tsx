// Wired to F6 #19 / #23 (03-dd/api/F6_interview-bank.md): the server filters, sorts and pages, this view
// only sends `AdminListParams`; mock mode answers the same contract (entities/interview-question/api/
// admin-mock.ts). Selection is limited to the page on screen: any filter, sort or page change clears it
// (DD SHR0301 BR-SC-01). Layout and history below are from the prototype.
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

import { useState } from "react";
import Link from "next/link";
import { Gauge, Pencil, Tags, Trash2 } from "lucide-react";
import {
  levelTone,
  useInterviewLevels,
  useInterviewTopics,
  type AdminListParams,
  type AdminQuestionRow,
  type AdminSortKey,
} from "@/entities/interview-question";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { usePersistedPageSize, useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  BulkActionBar,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  EllipsisLink,
  ErrorState,
  IconAction,
  PageHeader,
  Pagination,
  Skeleton,
  FilterBar,
  FilterMenu,
  type DataTableColumn,
} from "@/shared/ui";
import {
  useAdminQuestionList,
  useQuestionUsage,
  useRetireQuestions,
} from "../api";
import { CsvImportDialog } from "./csv-import-dialog";
import { LevelManagerDialog } from "./level-manager-dialog";
import { TopicManagerDialog } from "./topic-manager-dialog";

// 8 is the default, 20 and 50 the other choices: same as the problem list (SHR0201 Q4).
const PAGE_SIZES = [8, 20, 50] as const;
const SEARCH_MAX_LENGTH = 100;

type SortKey =
  | "code"
  | "question"
  | "topic"
  | "level"
  | "followUps"
  | "rubric"
  | "usage"
  | "score";

// Column key on screen -> `sort` parameter of #19.
const SORT_PARAM: Record<SortKey, AdminSortKey> = {
  code: "displayNo",
  question: "title",
  topic: "topic",
  level: "level",
  followUps: "followUpCount",
  rubric: "rubricCount",
  usage: "usageCount",
  score: "averageScorePercent",
};

type Props = {
  /** Area root for row links and the create button, e.g. "/admin/interview-questions". */
  basePath: string;
  /** Topic management is ADMIN-only; the screen is shared with INSTRUCTOR, who may only pick topics. */
  canManageTopics?: boolean;
};

/** `IQ-014`: a label from `displayNo`, never a key (BD SHR0301 Q5). */
const codeLabel = (row: AdminQuestionRow) =>
  `IQ-${String(row.displayNo).padStart(3, "0")}`;

const labelOf = (
  list: readonly { key: string; label: string }[],
  ref: { code: string; displayName: string },
) => list.find((item) => item.key === ref.code)?.label ?? ref.displayName;

export function InterviewQuestionManagementView({
  basePath,
  canManageTopics = false,
}: Props) {
  const t = useT("interviewQuestionManagement");
  const apiMessage = useApiErrorMessage();
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const usage = useQuestionUsage();
  const retire = useRetireQuestions();
  const [managingTopics, setManagingTopics] = useState(false);
  const [managingLevels, setManagingLevels] = useState(false);
  const [query, setQuery] = useState("");
  const [topic, setTopic] = useState("all");
  const [level, setLevel] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-interview-questions-page-size",
    PAGE_SIZES,
    8,
  );
  // Empty key = no column sorted yet, so the bank keeps its natural order until a header is clicked.
  const [sort, setSort] = useState<{
    key: SortKey | "";
    direction: "asc" | "desc";
  }>({
    key: "",
    direction: "asc",
  });
  const [importingCsv, setImportingCsv] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<AdminQuestionRow | null>(
    null,
  );
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [confirmingBulkDelete, setConfirmingBulkDelete] = useState(false);

  // Typing filters live, but only a pause in typing becomes a request.
  const debouncedQuery = useDebounced(query);
  const params: AdminListParams = {
    q: debouncedQuery.trim().slice(0, SEARCH_MAX_LENGTH),
    topic,
    level,
    sort: sort.key ? SORT_PARAM[sort.key] : "",
    dir: sort.direction,
    page: currentPage,
    pageSize,
  };
  const list = useAdminQuestionList(params);
  const data = list.data;

  if (list.isError && !data)
    return (
      <ErrorState>
        <div className="flex flex-col items-center gap-3">
          <p>{t("loadFailed")}</p>
          <Button variant="ghost" onClick={() => void list.refetch()}>
            {t("retry")}
          </Button>
        </div>
      </ErrorState>
    );
  if (!data) {
    return (
      <div className="flex flex-col gap-3.5" aria-busy="true">
        <Skeleton className="h-[62px] w-full" />
        <Skeleton className="h-[360px] w-full" />
      </div>
    );
  }

  const rows = data.items;
  const busy = retire.isPending;

  // Any change of what the table shows starts from an empty selection.
  function change(apply: () => void, resetPage = true) {
    apply();
    if (resetPage) setCurrentPage(1);
    setSelected(new Set());
  }

  function toggleRow(id: string) {
    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  /** Partial success is a 200: report what went through, keep the failed ids selected. */
  function retireIds(ids: string[], single?: AdminQuestionRow) {
    retire.mutate(ids, {
      onSuccess: (result) => {
        const failed = new Set(result.failedItems.map((item) => item.id));
        setSelected(
          (previous) => new Set([...previous].filter((id) => failed.has(id))),
        );
        if (result.failedItems.length === 0) {
          toast.success(
            single
              ? t("toast.deleted", { code: codeLabel(single) })
              : t("toast.bulkDeleted", { count: result.retiredIds.length }),
          );
          return;
        }
        toast.warning(
          t("toast.bulkPartial", {
            done: result.retiredIds.length,
            failed: result.failedItems.length,
            reason: apiMessage(new ApiError(result.failedItems[0]!.code, 0)),
          }),
        );
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  // Live filtering stays silent; only an explicit submit (Enter) reports the result count.
  function announceSearch() {
    const count = data!.page.totalItems;
    toast.info(
      count === 0 ? t("toast.searchEmpty") : t("toast.searchResult", { count }),
    );
  }

  const columns: DataTableColumn<AdminQuestionRow>[] = [
    {
      key: "code",
      sortable: true,
      header: t("columnCode"),
      width: "92px",
      render: (question) => (
        <span className="font-mono text-xs whitespace-nowrap text-[var(--color-text-muted)]">
          {codeLabel(question)}
        </span>
      ),
    },
    {
      key: "question",
      sortable: true,
      header: t("columnQuestion"),
      width: "38%",
      render: (question) => (
        <EllipsisLink
          href={`${basePath}/${question.id}`}
          className="font-semibold"
        >
          {question.title}
        </EllipsisLink>
      ),
    },
    {
      key: "topic",
      sortable: true,
      header: t("columnTopic"),
      width: "140px",
      render: (question) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {labelOf(topicList, question.topic)}
        </span>
      ),
    },
    {
      key: "level",
      sortable: true,
      header: t("columnLevel"),
      width: "116px",
      render: (question) => (
        <Badge variant={levelTone(levelList, question.level.code)}>
          {labelOf(levelList, question.level)}
        </Badge>
      ),
    },
    {
      key: "followUps",
      sortable: true,
      header: t("columnFollowUps"),
      width: "84px",
      align: "right",
      render: (question) => (
        <span className="font-mono">{question.followUpCount}</span>
      ),
    },
    {
      key: "rubric",
      sortable: true,
      header: t("columnRubric"),
      width: "84px",
      align: "right",
      render: (question) => (
        <span className="font-mono">{question.rubricCount}</span>
      ),
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
      // null until the first completed practice attempt (DD F6 19.3).
      render: (question) => (
        <span className="font-mono font-semibold">
          {question.averageScorePercent === null
            ? "-"
            : `${Math.round(question.averageScorePercent)}%`}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "92px",
      align: "right",
      render: (question) => (
        <span className="flex justify-end gap-1">
          <IconAction
            icon={Pencil}
            label={t("edit")}
            ariaLabel={t("editQuestion", { code: codeLabel(question) })}
            href={`${basePath}/${question.id}/edit`}
          />
          <IconAction
            icon={Trash2}
            label={t("delete")}
            ariaLabel={t("deleteQuestion", { code: codeLabel(question) })}
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
        description={t("subtitle", { total: data.total })}
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
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => setImportingCsv(true)}
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
            onChange: (next) => change(() => setQuery(next)),
            onSubmit: announceSearch,
          }}
          resultCount={t("resultCount", {
            shown: data.page.totalItems,
            total: data.total,
          })}
        >
          <FilterMenu
            label={t("topicFilterLabel")}
            value={topic}
            onValueChange={(value) => change(() => setTopic(value))}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...topicList.map((topic) => ({
                value: topic.key,
                label: topic.label,
              })),
            ]}
          />
          <FilterMenu
            label={t("levelFilterLabel")}
            value={level}
            onValueChange={(value) => change(() => setLevel(value))}
            options={[
              { value: "all" as const, label: t("filterAll") },
              ...levelList.map((item) => ({
                value: item.key,
                label: item.label,
              })),
            ]}
          />
        </FilterBar>

        <BulkActionBar
          count={selected.size}
          label={t("selectionLabel", { count: selected.size })}
        >
          {/* Bulk delete only. There is no duplicate action anywhere on this list (owner, 2026-10-08). */}
          <Button
            variant="ghost"
            size="sm"
            className="text-[var(--color-admin-negative-text)]"
            disabled={busy}
            onClick={() => setConfirmingBulkDelete(true)}
          >
            {t("bulk.delete")}
          </Button>
        </BulkActionBar>

        <DataTable
          caption={t("tableCaption")}
          columns={columns}
          rows={rows}
          rowKey={(question) => question.id}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: (selectAll) =>
              setSelected(
                selectAll ? new Set(rows.map((row) => row.id)) : new Set(),
              ),
            selectAllLabel: t("selectAll"),
            rowLabel: (question) =>
              t("selectRow", { code: codeLabel(question) }),
          }}
          emptyMessage={data.total === 0 ? t("emptyBank") : t("emptyFiltered")}
          minWidth={1000}
          sort={{
            key: sort.key,
            direction: sort.direction,
            onSortChange: (key, direction) =>
              change(() => setSort({ key: key as SortKey, direction })),
          }}
        />

        <Pagination
          page={data.page.number}
          pageSize={pageSize}
          pageSizeOptions={PAGE_SIZES}
          pageSizeLabel={t("pageSizeLabel")}
          onPageSizeChange={(size) => change(() => setPageSize(size))}
          total={data.page.totalItems}
          onPageChange={(next) => change(() => setCurrentPage(next), false)}
          summary={t("pageLabel", {
            page: data.page.number,
            totalPages: data.page.totalPages,
            shown: rows.length,
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
        usage={usage.data?.topics ?? {}}
      />

      <LevelManagerDialog
        open={managingLevels}
        onClose={() => setManagingLevels(false)}
        usage={usage.data?.levels ?? {}}
      />

      <CsvImportDialog
        open={importingCsv}
        onClose={() => setImportingCsv(false)}
        existing={rows.map((question) => question.title)}
      />

      <ConfirmDialog
        open={confirmingBulkDelete}
        onClose={() => setConfirmingBulkDelete(false)}
        onConfirm={() => {
          retireIds([...selected]);
          setConfirmingBulkDelete(false);
        }}
        title={t("confirmBulkDeleteTitle", { count: selected.size })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) retireIds([pendingDelete.id], pendingDelete);
          setPendingDelete(null);
        }}
        title={t("confirmDeleteTitle", {
          code: pendingDelete ? codeLabel(pendingDelete) : "",
        })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
      </ConfirmDialog>
    </div>
  );
}
