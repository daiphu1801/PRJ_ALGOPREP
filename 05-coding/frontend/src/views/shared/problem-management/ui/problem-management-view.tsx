// SHR0201 on the real api (03-dd/api/F2_problem-bank.md #24 to #32); mock mode answers the same contract.
//
// Layout follows 09-layoutBase/Admin - Quản lý bài tập.dc.html: sticky header with the "Bài tập
// mới" CTA (:149-159), then the problem table card with filters, a bulk action bar, a sortable table
// and numbered paging (:175-250). The stat cards (:161-173) were dropped (owner rule 2026-10-13); the
// two totals live in the header line.
//
// Shared screen: A2 and A3 both use it, mounted at /instructor and /admin
// (DEC-2026-0825-shared-content-authoring-screens). `basePath` is the area's problems root
// ("/admin/problems" or "/instructor/problems"), so row links stay inside the area they were opened from.
//
// The server filters, sorts and pages; this view only sends `AdminListParams`. Selection is limited to
// the page on screen (any filter, sort or page change clears it), so a bulk action and the delete warning
// always name exactly the rows the admin can see. A bulk call handles each problem on its own: a
// problem that fails is reported with the server's reason, the rest still go through.
//
// TWO lifecycle states, not three. DEC-2026-0830-problem-lifecycle-two-states drops "Đã ẩn"; the
// mockup still has it on row #987 and inside a bulk action labelled "Xuất bản / ẩn". The filter
// therefore offers draft/published only, and the bulk action is "Xuất bản".
"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Gauge, Pencil, Tags, Trash2 } from "lucide-react";
import {
  useAdminProblemList,
  useAttentionList,
  useBulkDelete,
  useBulkDifficulty,
  useBulkPublish,
  useBulkUnpublish,
  useBulkTopic,
  useExportFilteredProblems,
  useExportProblems,
  useTopicDistribution,
  type AdminListParams,
  type AdminProblem,
  type BulkResult,
  type ProblemSortKey,
  type ProblemStatus,
} from "../api";
import {
  problemLevelLabel,
  problemLevelTone,
  problemTopicLabel,
  useProblemLevels,
  useProblemTopicActions,
  useProblemTopics,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import {
  formatRelativeVN,
  saveBlob,
  useDebounced,
  usePersistedPageSize,
} from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  BulkActionBar,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  EllipsisLink,
  IconAction,
  ManagedListDialog,
  ErrorState,
  PageHeader,
  Skeleton,
  Pagination,
  ProgressBar,
  FilterBar,
  FilterMenu,
  SelectField,
  type DataTableColumn,
} from "@/shared/ui";
import { BulkLevelDialog } from "./bulk-level-dialog";
import { LevelManagerDialog } from "./level-manager-dialog";

// The dot keeps the vivid accent; the label text needs the darker `-text` token to reach AA contrast.
const STATUS_COLOR_VAR: Record<ProblemStatus, string> = {
  published: "--color-success",
  draft: "--color-admin-warn",
};
const STATUS_TEXT_VAR: Record<ProblemStatus, string> = {
  published: "--color-success-text",
  draft: "--color-admin-warn-text",
};

// 8 is the default, 20 and 50 the other choices (SHR0201 Q4); the server rejects any other value.
const PAGE_SIZES = [8, 20, 50] as const;

// The server publishes 1 to 20 problems per call: each one runs the reference solution in the sandbox.
const PUBLISH_MAX = 20;
const SEARCH_MAX_LENGTH = 100;

type BulkAction =
  "publish" | "changeDifficulty" | "assignTopic" | "duplicate" | "exportCsv";
type BulkKind = "publish" | "delete" | "changeDifficulty" | "unpublish" | "assignTopic";
type StatusFilter = ProblemStatus | "all";

type Props = {
  /** Area root for row links and the create button, e.g. "/admin/problems". */
  basePath: string;
  /** Topic management is ADMIN-only; the screen is shared with INSTRUCTOR, who may only pick topics. */
  canManageTopics?: boolean;
};

export function ProblemManagementView({
  basePath,
  canManageTopics = false,
}: Props) {
  const t = useT("problemManagement");
  const locale = useLocale() === "en" ? "en" : "vi";
  const apiMessage = useApiErrorMessage();
  const router = useRouter();
  const topicList = useProblemTopics();
  const levelList = useProblemLevels();
  const topicActions = useProblemTopicActions();
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-problems-page-size",
    PAGE_SIZES,
    8,
  );
  const [managingTopics, setManagingTopics] = useState(false);
  const [managingLevels, setManagingLevels] = useState(false);
  const [pickingLevel, setPickingLevel] = useState(false);
  const [pickingTopic, setPickingTopic] = useState(false);
  const [pickedTopic, setPickedTopic] = useState("");
  const [confirmingUnpublish, setConfirmingUnpublish] = useState(false);
  const [query, setQuery] = useState("");
  const [level, setLevel] = useState("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [bulkFailures, setBulkFailures] = useState<BulkResult["failedItems"]>([]);
  const [sort, setSort] = useState<{
    key: ProblemSortKey;
    direction: "asc" | "desc";
  }>({ key: "edited", direction: "desc" });
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingDelete, setPendingDelete] = useState<AdminProblem | null>(null);
  const [confirmingBulkDelete, setConfirmingBulkDelete] = useState(false);

  // Typing filters live, but only a pause in typing becomes a request.
  const debouncedQuery = useDebounced(query);
  const params: AdminListParams = {
    q: debouncedQuery.trim().slice(0, SEARCH_MAX_LENGTH),
    level,
    status,
    sort: sort.key,
    dir: sort.direction,
    page: currentPage,
    pageSize,
  };
  const list = useAdminProblemList(params);
  const distribution = useTopicDistribution();
  const attention = useAttentionList();
  const publish = useBulkPublish();
  const unpublish = useBulkUnpublish();
  const assignTopic = useBulkTopic();
  const exportFiltered = useExportFilteredProblems();
  const remove = useBulkDelete();
  const changeLevel = useBulkDifficulty();
  const exportCsv = useExportProblems();

  const data = list.data;
  const rows = data?.items ?? [];
  // The selection only ever holds rows of the page on screen.
  const chosen = rows.filter((problem) => selected.has(problem.id));
  const busy =
    publish.isPending || unpublish.isPending || assignTopic.isPending || exportFiltered.isPending ||
    remove.isPending ||
    changeLevel.isPending ||
    exportCsv.isPending;

  // Any change of what the table shows starts from an empty selection.
  function change(apply: () => void, resetPage = true) {
    apply();
    if (resetPage) setCurrentPage(1);
    setSelected(new Set());
  }

  const levelUsage = Object.fromEntries(
    levelList.map((item) => [
      item.key,
      item.problemCount ?? data?.usage?.levels[item.key] ?? 0,
    ]),
  );
  const topicUsage = Object.fromEntries(
    topicList.map((item) => [
      item.key,
      item.problemCount ?? data?.usage?.topics[item.key] ?? 0,
    ]),
  );

  // Live filtering stays silent; only an explicit submit (Enter) reports the result count.
  function announceSearch() {
    const count = data?.page.totalItems ?? 0;
    toast.info(
      count === 0 ? t("toast.searchEmpty") : t("toast.searchResult", { count }),
    );
  }

  /** One toast for a bulk answer: all done, or what went through and why the others did not. */
  function reportBulk(kind: BulkKind, result: BulkResult) {
    setSelected(new Set());
    const done = result.succeededIds.length;
    const failed = result.failedItems;
    setBulkFailures(failed);
    if (failed.length === 0) {
      toast.success(t(`toast.bulk.${kind}`, { count: done }));
      return;
    }
    toast.warning(
      t("toast.bulkPartial", {
        done,
        failed: failed.length,
        reason: apiMessage(new ApiError(failed[0]!.code, 0)),
      }),
    );
  }

  function runBulk(action: BulkAction) {
    if (action === "changeDifficulty") {
      setPickingLevel(true);
      return;
    }
    if (action === "assignTopic") {
      setPickedTopic(topicList[0]?.key ?? "");
      setPickingTopic(true);
      return;
    }
    if (action === "duplicate") {
      // F2-16: the copy is an authoring form pre-filled from the source; nothing is created until Save.
      // One source at a time, as opening several forms at once makes no sense.
      if (chosen.length !== 1) {
        toast.info(t("toast.duplicatePickOne"));
        return;
      }
      router.push(`${basePath}/new?from=${encodeURIComponent(chosen[0]!.id)}`);
      return;
    }
    const ids = chosen.map((problem) => problem.id);
    if (action === "publish") {
      if (ids.length > PUBLISH_MAX) {
        toast.warning(t("toast.publishMax", { max: PUBLISH_MAX }));
        return;
      }
      publish.mutate(ids, {
        onSuccess: (result) => reportBulk("publish", result),
        onError: (error) => toast.error(apiMessage(error)),
      });
      return;
    }
    exportCsv.mutate(ids, {
      onSuccess: ({ blob, filename }) => {
        saveBlob(blob, filename ?? "problems.csv");
        toast.success(t("toast.exported", { count: ids.length }));
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  function applyLevel(levelKey: string) {
    const ids = chosen.map((problem) => problem.id);
    setPickingLevel(false);
    changeLevel.mutate(
      { ids, levelKey },
      {
        onSuccess: (result) => reportBulk("changeDifficulty", result),
        onError: (error) => toast.error(apiMessage(error)),
      },
    );
  }

  function deleteChosen() {
    remove.mutate(
      chosen.map((problem) => problem.id),
      {
        onSuccess: (result) => reportBulk("delete", result),
        onError: (error) => toast.error(apiMessage(error)),
      },
    );
  }

  function deleteOne(problem: AdminProblem) {
    remove.mutate([problem.id], {
      onSuccess: (result) => {
        // A deleted row must not stay counted in the selection.
        setSelected((previous) => {
          const next = new Set(previous);
          next.delete(problem.id);
          return next;
        });
        if (result.failedItems.length === 0) {
          toast.success(t("toast.deleted", { title: problem.title }));
        } else {
          toast.warning(
            t("toast.bulkPartial", {
              done: 0,
              failed: 1,
              reason: apiMessage(new ApiError(result.failedItems[0]!.code, 0)),
            }),
          );
        }
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  // What the bulk delete hides from learners, shown as a warning (never blocks, BD SHR0201 Q7).
  const bulkDeleteImpact = {
    submissions: chosen.reduce(
      (sum, problem) => sum + problem.submissionCount,
      0,
    ),
    assigned: chosen.filter((problem) => problem.assignedClassCount > 0).length,
  };

  function toggleRow(id: string) {
    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const columns: DataTableColumn<AdminProblem>[] = [
    {
      key: "code",
      header: t("columnCode"),
      width: "92px",
      sortable: true,
      render: (problem) => (
        <span className="font-mono text-xs text-[var(--color-text-muted)]">
          {problem.code}
        </span>
      ),
    },
    {
      key: "title",
      header: t("columnTitle"),
      width: "30%",
      sortable: true,
      render: (problem) => (
        <EllipsisLink
          href={`${basePath}/${problem.id}`}
          className="font-semibold"
        >
          {problem.title}
        </EllipsisLink>
      ),
    },
    {
      key: "topic",
      header: t("columnTopic"),
      width: "132px",
      render: (problem) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {problem.topics
            .map((topic) => problemTopicLabel(topicList, topic))
            .join(", ")}
        </span>
      ),
    },
    {
      key: "difficulty",
      header: t("columnDifficulty"),
      width: "108px",
      sortable: true,
      render: (problem) => (
        <Badge variant={problemLevelTone(levelList, problem.difficulty)}>
          {problemLevelLabel(levelList, problem.difficulty)}
        </Badge>
      ),
    },
    {
      key: "status",
      header: t("columnStatus"),
      width: "124px",
      sortable: true,
      render: (problem) => (
        <span
          className="flex items-center gap-1.5 text-[12.5px] font-semibold whitespace-nowrap"
          style={{ color: `var(${STATUS_TEXT_VAR[problem.status]})` }}
        >
          <span
            aria-hidden="true"
            className="inline-block h-1.5 w-1.5 shrink-0 rounded-full"
            style={{ background: `var(${STATUS_COLOR_VAR[problem.status]})` }}
          />
          {t(`status.${problem.status}`)}
        </span>
      ),
    },
    {
      key: "submissions",
      // Auto table layout treats the width as a hint, so the label itself must not wrap (admin UX
      // review F11).
      header: (
        <span className="whitespace-nowrap">{t("columnSubmissions")}</span>
      ),
      width: "100px",
      align: "right",
      sortable: true,
      render: (problem) => (
        <span className="font-mono text-[var(--color-text-muted)]">
          {problem.submissionCount.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "accepted",
      header: t("columnAccepted"),
      width: "76px",
      align: "right",
      sortable: true,
      render: (problem) => (
        <span className="font-mono font-semibold">
          {/* No statistics row yet is "-", never "0%" (DD F2 1.2). */}
          {problem.acceptedRate === null ? "-" : `${problem.acceptedRate}%`}
        </span>
      ),
    },
    {
      key: "edited",
      header: t("columnTestcasesAndEdit"),
      width: "116px",
      align: "right",
      sortable: true,
      render: (problem) => (
        // Two deliberate lines instead of a wrap that split "3 ngày" (admin UX review F11).
        <span className="flex flex-col font-mono text-xs leading-tight text-[var(--color-text-muted)]">
          <span className="whitespace-nowrap">
            {t("testcasesCount", { count: problem.testcaseCount })}
          </span>
          <span className="whitespace-nowrap">
            {formatRelativeVN(problem.updatedAt, { locale })}
          </span>
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "88px",
      align: "right",
      render: (problem) => (
        <span className="flex justify-end gap-1">
          <IconAction
            icon={Pencil}
            label={t("edit")}
            ariaLabel={t("editProblem", { title: problem.title })}
            href={`${basePath}/${problem.id}/edit`}
          />
          <IconAction
            icon={Trash2}
            label={t("delete")}
            ariaLabel={t("deleteProblem", { title: problem.title })}
            tone="danger"
            onClick={() => setPendingDelete(problem)}
          />
        </span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={
          data
            ? t("subtitle", {
                total: data.totals.total,
                published: data.totals.published,
              })
            : undefined
        }
        actions={
          <>
            <Button variant="ghost" size="sm" disabled={busy || !data || list.isError}
              onClick={() => exportFiltered.mutate(params, {
                onSuccess: ({blob, filename}) => saveBlob(blob, filename ?? "problems.csv"),
                onError: error => toast.error(apiMessage(error)),
              })}>{t("exportFiltered")}</Button>
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
            <Button asChild variant="cta" size="sm">
              <Link href={`${basePath}/new`}>{t("newProblem")}</Link>
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
            maxLength: SEARCH_MAX_LENGTH,
            onChange: (next) => change(() => setQuery(next)),
            onSubmit: announceSearch,
          }}
          resultCount={
            data
              ? t("resultCount", {
                  shown: data.page.totalItems,
                  total: data.totals.total,
                })
              : undefined
          }
        >
          <FilterMenu
            label={t("difficultyFilterLabel")}
            value={level}
            onValueChange={(value) => change(() => setLevel(value))}
            options={[
              { value: "all", label: t("filterAll") },
              ...levelList.map((item) => ({
                value: item.key,
                label: item.label,
              })),
            ]}
          />
          <FilterMenu
            label={t("statusFilterLabel")}
            value={status}
            onValueChange={(value) => change(() => setStatus(value))}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "published", label: t("status.published") },
              { value: "draft", label: t("status.draft") },
            ]}
          />
        </FilterBar>

        <BulkActionBar
          count={chosen.length}
          label={t("selectionLabel", { count: chosen.length })}
        >
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            disabled={busy}
            onClick={() => setConfirmingUnpublish(true)}
          >
            {t("bulk.unpublish")}
          </Button>
          {/* "Xuất bản", not the mockup's "Xuất bản / ẩn": hiding is no longer a state. */}
          {(
            [
              "publish",
              "changeDifficulty",
              "assignTopic",
              "duplicate",
              "exportCsv",
            ] as const satisfies readonly BulkAction[]
          ).map((action) => (
            <Button
              key={action}
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              disabled={busy}
              onClick={() => runBulk(action)}
            >
              {t(`bulk.${action}`)}
            </Button>
          ))}
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

        {bulkFailures.length > 0 ? <Card title={t("bulkFailureTitle")}>
          <ul className="list-disc pl-5 text-sm" role="status">
            {bulkFailures.map(item => <li key={item.id}>
              {rows.find(row => row.id === item.id)?.title ?? item.id}: {apiMessage(new ApiError(item.code, 0))}
            </li>)}
          </ul>
        </Card> : null}
        {list.isError ? (
          <ErrorState>{t("loadFailed")}</ErrorState>
        ) : !data ? (
          <div className="flex flex-col gap-3.5" aria-busy="true">
            <Skeleton className="h-[360px] w-full" />
          </div>
        ) : (
          <>
            <DataTable
              caption={t("tableCaption")}
              columns={columns}
              rows={rows}
              rowKey={(problem) => problem.id}
              emptyMessage={t("emptyFiltered")}
              minWidth={1060}
              selection={{
                selectedKeys: selected,
                onToggleRow: toggleRow,
                onToggleAll: (selectAll) =>
                  setSelected(
                    selectAll ? new Set(rows.map((row) => row.id)) : new Set(),
                  ),
                selectAllLabel: t("selectAll"),
                rowLabel: (problem) => t("selectRow", { title: problem.title }),
              }}
              sort={{
                key: sort.key,
                direction: sort.direction,
                onSortChange: (key, direction) =>
                  change(() =>
                    setSort({ key: key as ProblemSortKey, direction }),
                  ),
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
          </>
        )}
      </Card>

      <div className="mt-4 grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(320px,1fr))]">
        <Card
          title={t("topicDist.title")}
          description={
            distribution.data
              ? t("topicDist.subtitle", {
                  total: distribution.data.totalProblemsWithTopic,
                  topics: distribution.data.totalTopicsUsed,
                })
              : undefined
          }
        >
          {distribution.isError ? (
            <ErrorState>{t("blockFailed")}</ErrorState>
          ) : !distribution.data ? (
            <Skeleton className="h-24 w-full" />
          ) : (
            <ul className="flex flex-col gap-3.5">
              {distribution.data.items.map((item) => (
                <li key={item.topicName}>
                  <div className="mb-1.5 flex items-center justify-between gap-2.5">
                    <span className="text-[13px] font-semibold">
                      {problemTopicLabel(topicList, item.topicName)}
                    </span>
                    <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
                      {item.count} · {item.percent}%
                    </span>
                  </div>
                  <ProgressBar
                    value={item.percent}
                    label={problemTopicLabel(topicList, item.topicName)}
                    height={7}
                  />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card
          title={t("attention.title")}
          action={
            <span className="text-[12.5px] text-[var(--color-text-subtle)]">
              {t("attention.tag")}
            </span>
          }
        >
          {attention.isError ? (
            <ErrorState>{t("blockFailed")}</ErrorState>
          ) : !attention.data ? (
            <Skeleton className="h-24 w-full" />
          ) : (
            <ul className="flex flex-col gap-2.5">
              {attention.data
                .filter((item) => item.count > 0)
                .map((item) => (
                  <li
                    key={item.ruleCode}
                    className="glass-surface flex items-center gap-3 rounded-2xl border border-[var(--color-border)] px-3.5 py-2.5"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="text-[13.5px] font-semibold">
                        {t(`attention.rule.${item.ruleCode}.title`)}
                      </div>
                      <div className="text-xs text-[var(--color-text-subtle)]">
                        {t(`attention.rule.${item.ruleCode}.meta`)}
                      </div>
                    </div>
                    <span
                      className="font-mono text-[13px] font-semibold"
                      style={{
                        color:
                          item.ruleCode === "noTestcase"
                            ? "var(--color-admin-negative-text)"
                            : "var(--color-admin-warn-text)",
                      }}
                    >
                      {item.count}
                    </span>
                  </li>
                ))}
            </ul>
          )}
        </Card>
      </div>

      <ManagedListDialog
        open={managingTopics}
        onClose={() => setManagingTopics(false)}
        items={topicList}
        usage={topicUsage}
        onAdd={topicActions.add}
        onRename={topicActions.rename}
        onRemove={topicActions.remove}
        onMove={topicActions.move}
        labels={{
          title: t("topicManager.title"),
          hint: t("topicManager.hint"),
          nameLabel: t("topicManager.nameLabel"),
          newLabel: t("topicManager.newLabel"),
          newPlaceholder: t("topicManager.newPlaceholder"),
          add: t("topicManager.add"),
          save: t("topicManager.save"),
          delete: t("delete"),
          close: t("topicManager.close"),
          usage: (count) => t("topicManager.usage", { count }),
          deleteBlocked: (count) => t("topicManager.deleteBlocked", { count }),
          moveUp: t("topicManager.moveUp"),
          moveDown: t("topicManager.moveDown"),
          error: {
            empty: t("topicManager.error.empty"),
            duplicate: t("topicManager.error.duplicate"),
          },
          done: {
            add: t("topicManager.done.add"),
            rename: t("topicManager.done.rename"),
            remove: t("topicManager.done.remove"),
          },
        }}
      />

      <LevelManagerDialog
        open={managingLevels}
        onClose={() => setManagingLevels(false)}
        usage={levelUsage}
      />

      <BulkLevelDialog
        open={pickingLevel}
        onClose={() => setPickingLevel(false)}
        count={chosen.length}
        onApply={applyLevel}
      />

      <ConfirmDialog open={pickingTopic} onClose={() => setPickingTopic(false)}
        title={t("bulkTopicTitle", {count: chosen.length})} confirmLabel={t("bulkLevel.apply")} cancelLabel={t("cancel")}
        pending={assignTopic.isPending} confirmDisabled={!pickedTopic || chosen.length === 0 || !topicList.some(item => item.key === pickedTopic)}
        onConfirm={() => assignTopic.mutate({ids: chosen.map(item => item.id), topicId: pickedTopic}, {
          onSuccess: result => { reportBulk("assignTopic", result); setPickingTopic(false); },
          onError: error => toast.error(apiMessage(error)),
        })}>
        <SelectField label={t("bulkTopicLabel")} value={pickedTopic} onChange={event => setPickedTopic(event.target.value)}
          options={topicList.map(item => ({value: item.key, label: item.label}))} />
      </ConfirmDialog>
      <ConfirmDialog open={confirmingUnpublish} onClose={() => setConfirmingUnpublish(false)}
        title={t("confirmUnpublish", {count: chosen.length})} confirmLabel={t("bulk.unpublish")} cancelLabel={t("cancel")}
        pending={unpublish.isPending} confirmDisabled={chosen.length === 0}
        onConfirm={() => unpublish.mutate(chosen.map(item => item.id), {
          onSuccess: result => { reportBulk("unpublish", result); setConfirmingUnpublish(false); },
          onError: error => toast.error(apiMessage(error)),
        })}>{t("unpublishHint")}</ConfirmDialog>
      <ConfirmDialog
        open={confirmingBulkDelete}
        onClose={() => setConfirmingBulkDelete(false)}
        onConfirm={() => {
          deleteChosen();
          setConfirmingBulkDelete(false);
        }}
        title={t("confirmBulkDeleteTitle", { count: chosen.length })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
        <span className="mt-2 block font-semibold text-[var(--color-admin-warn-text)]">
          {t("confirmBulkDeleteImpact", {
            count: chosen.length,
            submissions: bulkDeleteImpact.submissions,
            assigned: bulkDeleteImpact.assigned,
          })}
        </span>
      </ConfirmDialog>

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) deleteOne(pendingDelete);
          setPendingDelete(null);
        }}
        title={t("confirmDeleteTitle", { title: pendingDelete?.title ?? "" })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
        {pendingDelete &&
        (pendingDelete.submissionCount > 0 ||
          pendingDelete.assignedClassCount > 0) ? (
          <span className="mt-2 block font-semibold text-[var(--color-admin-warn-text)]">
            {t("confirmDeleteImpact", {
              submissions: pendingDelete.submissionCount,
              classes: pendingDelete.assignedClassCount,
            })}
          </span>
        ) : null}
      </ConfirmDialog>
    </div>
  );
}
