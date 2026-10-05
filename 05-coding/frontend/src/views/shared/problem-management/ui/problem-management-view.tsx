// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Quản lý bài tập.dc.html: sticky header with the "Bài tập
// mới" CTA (:149-159), stat cards (:161-173), then the problem table card with filters, a bulk
// action bar, a sortable table and numbered paging (:175-250).
//
// Shared screen: A2 and A3 both use it, mounted at /instructor and /admin
// (DEC-2026-0825-shared-content-authoring-screens). `basePath` is the area's problems root
// ("/admin/problems" or "/instructor/problems"), so row links stay inside the area they were opened from.
//
// TWO lifecycle states, not three. DEC-2026-0830-problem-lifecycle-two-states drops "Đã ẩn"; the
// mockup still has it on row #987 and inside a bulk action labelled "Xuất bản / ẩn". The filter
// therefore offers draft/published only, and the bulk action is "Xuất bản".
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Gauge, Pencil, Tags, Trash2 } from "lucide-react";
import {
  useAdminProblemPage,
  type AdminProblemPage,
  type AdminProblem,
  type Difficulty,
  type ProblemSortKey,
  type ProblemStatus,
} from "../api";
import {
  addProblemTopic,
  problemLevelLabel,
  problemLevelRank,
  problemLevelTone,
  problemTopicLabel,
  moveProblemTopic,
  removeProblemTopic,
  renameProblemTopic,
  useProblemLevels,
  useProblemTopics,
} from "@/entities/problem";
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
  ManagedListDialog,
  ErrorState,
  PageHeader,
  Skeleton,
  Pagination,
  ProgressBar,
  FilterBar,
  FilterMenu,
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

// 8 is the default, 20 and 50 the other choices (SHR0201 Q4).
const PAGE_SIZES = [8, 20, 50] as const;


type BulkAction = "publish" | "changeDifficulty" | "assignTopic" | "duplicate" | "exportCsv";
type DifficultyFilter = Difficulty | "all";
type StatusFilter = ProblemStatus | "all";

type Props = {
  /** Area root for row links and the create button, e.g. "/admin/problems". */
  basePath: string;
  /** Topic management is ADMIN-only; the screen is shared with INSTRUCTOR, who may only pick topics. */
  canManageTopics?: boolean;
};

/** Loads the list, then hands it to the form that owns filters, selection and local deletes. */
export function ProblemManagementView(props: Props) {
  const t = useT("problemManagement");
  const query = useAdminProblemPage();

  if (query.isError) return <ErrorState>{t("loadFailed")}</ErrorState>;
  if (!query.data) {
    return (
      <div className="flex flex-col gap-3.5" aria-busy="true">
        <Skeleton className="h-[62px] w-full" />
        <Skeleton className="h-[360px] w-full" />
      </div>
    );
  }
  return <ProblemManagementForm {...props} page={query.data} />;
}

function ProblemManagementForm({
  basePath,
  canManageTopics = false,
  page,
}: Props & { page: AdminProblemPage }) {
  const t = useT("problemManagement");
  const topicList = useProblemTopics();
  const levelList = useProblemLevels();
  const [pageSize, setPageSize] = usePersistedPageSize("algoprep-problems-page-size", PAGE_SIZES, 8);
  const [managingTopics, setManagingTopics] = useState(false);
  const [managingLevels, setManagingLevels] = useState(false);
  const [pickingLevel, setPickingLevel] = useState(false);
  // Level changes made through the bulk action; the mock rows themselves are read-only.
  const [levelOverrides, setLevelOverrides] = useState<Readonly<Record<string, string>>>({});
  const levelOf = (problem: AdminProblem) => levelOverrides[problem.code] ?? problem.difficulty;
  const [query, setQuery] = useState("");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [sort, setSort] = useState<{ key: ProblemSortKey; direction: "asc" | "desc" }>({
    key: "edited",
    direction: "desc",
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingDelete, setPendingDelete] = useState<AdminProblem | null>(null);
  const [confirmingBulkDelete, setConfirmingBulkDelete] = useState(false);
  const [deletedCodes, setDeletedCodes] = useState<ReadonlySet<string>>(new Set());

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = page.problems.filter(
      (problem) =>
        !deletedCodes.has(problem.code) &&
        (difficulty === "all" || levelOf(problem) === difficulty) &&
        (status === "all" || problem.status === status) &&
        (!needle ||
          problem.code.toLowerCase().includes(needle) ||
          problem.title.toLowerCase().includes(needle)),
    );

    const factor = sort.direction === "asc" ? 1 : -1;
    // `edited` is a relative label, not a date — the mock is already newest-first, so sorting by it
    // only reverses. Everything else compares the real value.
    if (sort.key === "edited") {
      return sort.direction === "asc" ? [...rows].reverse() : rows;
    }
    return [...rows].sort((left, right) => {
      switch (sort.key) {
        case "submissions":
          return (left.submissionCount - right.submissionCount) * factor;
        case "accepted":
          return (left.acceptedRate - right.acceptedRate) * factor;
        case "difficulty": {
          // Admin-chosen order of the level list, not a fixed easy < medium < hard.
          return (problemLevelRank(levelList, levelOf(left)) - problemLevelRank(levelList, levelOf(right))) * factor;
        }
        case "status":
          return left.status.localeCompare(right.status) * factor;
        case "topic":
          return left.topic.localeCompare(right.topic, "vi") * factor;
        default:
          return left.title.localeCompare(right.title, "vi") * factor;
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- levelOf only reads levelOverrides
  }, [page.problems, deletedCodes, query, difficulty, status, sort, levelList, levelOverrides]);

  // Rows still on screen (not locally deleted) per topic, so the manager can refuse deleting a topic
  // that is in use.
  const topicUsage = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const problem of page.problems) {
      if (!deletedCodes.has(problem.code)) counts[problem.topic] = (counts[problem.topic] ?? 0) + 1;
    }
    return counts;
  }, [page.problems, deletedCodes]);

  const levelUsage = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const problem of page.problems) {
      if (deletedCodes.has(problem.code)) continue;
      const key = levelOverrides[problem.code] ?? problem.difficulty;
      counts[key] = (counts[key] ?? 0) + 1;
    }
    return counts;
  }, [page.problems, deletedCodes, levelOverrides]);

  // Live filtering stays silent; only an explicit submit (Enter) reports the result count.
  function announceSearch() {
    toast.info(
      filtered.length === 0
        ? t("toast.searchEmpty")
        : t("toast.searchResult", { count: filtered.length }),
    );
  }

  function runBulk(action: BulkAction) {
    const count = selected.size;
    if (action === "changeDifficulty") {
      setPickingLevel(true);
      return;
    }
    if (action === "assignTopic") {
      // ponytail: prototype has no picker dialog for the topic yet, so the click only explains that.
      toast.info(t("toast.bulkNeedsChoice"));
      return;
    }
    // publish / duplicate / exportCsv have no API behind them yet: say so rather than report success.
    toast.info(t("toast.notWired", { action: t(`bulk.${action}`), count }));
  }

  function applyLevel(levelKey: string) {
    setLevelOverrides((previous) => ({
      ...previous,
      ...Object.fromEntries([...selected].map((code) => [code, levelKey])),
    }));
    setPickingLevel(false);
    toast.success(t("toast.bulk.changeDifficulty", { count: selected.size }));
  }

  function deleteSelected() {
    const count = selected.size;
    setDeletedCodes((previous) => new Set([...previous, ...selected]));
    setSelected(new Set());
    toast.success(t("toast.bulk.delete", { count }));
  }

  // Selection survives filter changes, so a bulk delete can reach rows the filter now hides.
  const hiddenSelectedCount = useMemo(() => {
    const shown = new Set(filtered.map((problem) => problem.code));
    return [...selected].filter((code) => !shown.has(code)).length;
  }, [filtered, selected]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  // Deleting rows can leave the stored page past the last one; clamp rather than render an empty page.
  const safePage = Math.min(currentPage, totalPages);
  const visible = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  function toggleRow(code: string) {
    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
  }

  const columns: DataTableColumn<AdminProblem>[] = [
    {
      key: "code",
      header: t("columnCode"),
      width: "92px",
      render: (problem) => (
        <span className="font-mono text-xs text-[var(--color-text-muted)]">{problem.code}</span>
      ),
    },
    {
      key: "title",
      header: t("columnTitle"),
      sortable: true,
      render: (problem) => (
        <Link
          href={`${basePath}/${problem.code.replace("#", "")}`}
          title={problem.title}
          className="block truncate font-semibold hover:underline"
        >
          {problem.title}
        </Link>
      ),
    },
    {
      key: "topic",
      header: t("columnTopic"),
      width: "132px",
      sortable: true,
      render: (problem) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {problemTopicLabel(topicList, problem.topic)}
        </span>
      ),
    },
    {
      key: "difficulty",
      header: t("columnDifficulty"),
      width: "108px",
      sortable: true,
      render: (problem) => (
        <Badge variant={problemLevelTone(levelList, levelOf(problem))}>
          {problemLevelLabel(levelList, levelOf(problem))}
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
          className="flex items-center gap-1.5 text-[12.5px] font-semibold"
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
      header: t("columnSubmissions"),
      width: "88px",
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
        <span className="font-mono font-semibold">{problem.acceptedRate}%</span>
      ),
    },
    {
      key: "edited",
      header: t("columnTestcasesAndEdit"),
      width: "116px",
      align: "right",
      sortable: true,
      render: (problem) => (
        <span className="font-mono text-xs text-[var(--color-text-muted)]">
          {t("testcasesAndEdit", { count: problem.testcaseCount, edited: problem.editedLabel })}
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
            href={`${basePath}/${problem.code.replace("#", "")}/edit`}
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
        description={t("subtitle", {
          total: page.totalProblems,
          published: page.publishedCount,
        })}
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
            onChange: (next) => {
              setQuery(next);
              setCurrentPage(1);
            },
            onSubmit: announceSearch,
          }}
          resultCount={t("resultCount", { shown: filtered.length, total: page.problems.length })}
        >
          <FilterMenu
            label={t("difficultyFilterLabel")}
            value={difficulty}
            onValueChange={(value) => {
              setDifficulty(value);
              setCurrentPage(1);
            }}
            options={[
              { value: "all", label: t("filterAll") },
              ...levelList.map((level) => ({ value: level.key, label: level.label })),
            ]}
          />
          <FilterMenu
            label={t("statusFilterLabel")}
            value={status}
            onValueChange={(value) => {
              setStatus(value);
              setCurrentPage(1);
            }}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "published", label: t("status.published") },
              { value: "draft", label: t("status.draft") },
            ]}
          />
        </FilterBar>

        <BulkActionBar count={selected.size} label={t("selectionLabel", { count: selected.size })}>
          {/* "Xuất bản", not the mockup's "Xuất bản / ẩn": hiding is no longer a state. */}
          {(["publish", "changeDifficulty", "assignTopic", "duplicate", "exportCsv"] as const satisfies readonly BulkAction[]).map(
            (action) => (
              <Button
                key={action}
                variant="ghost"
                size="sm"
                className="border border-[var(--color-border)]"
                onClick={() => runBulk(action)}
              >
                {t(`bulk.${action}`)}
              </Button>
            ),
          )}
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
          rowKey={(problem) => problem.code}
          emptyMessage={t("emptyFiltered")}
          minWidth={1060}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: (selectAll) =>
              setSelected(selectAll ? new Set(visible.map((row) => row.code)) : new Set()),
            selectAllLabel: t("selectAll"),
            rowLabel: (problem) => t("selectRow", { title: problem.title }),
          }}
          sort={{
            key: sort.key,
            direction: sort.direction,
            onSortChange: (key, direction) => {
              setSort({ key: key as ProblemSortKey, direction });
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

      <div className="mt-4 grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(320px,1fr))]">
        <Card
          title={t("topicDist.title")}
          description={t("topicDist.subtitle", {
            total: page.problems.length,
            topics: page.topicDistribution.length,
          })}
        >
          <ul className="flex flex-col gap-3.5">
            {page.topicDistribution.map((item) => (
              <li key={item.topicName}>
                <div className="mb-1.5 flex items-center justify-between gap-2.5">
                  <span className="text-[13px] font-semibold">
                    {problemTopicLabel(topicList, item.topicName)}
                  </span>
                  <span className="font-mono text-[12.5px] text-[var(--color-text-muted)]">
                    {item.count} · {item.percent}%
                  </span>
                </div>
                <ProgressBar value={item.percent} label={problemTopicLabel(topicList, item.topicName)} height={7} />
              </li>
            ))}
          </ul>
        </Card>

        <Card
          title={t("attention.title")}
          action={
            <span className="text-[12.5px] text-[var(--color-text-subtle)]">
              {t("attention.tag")}
            </span>
          }
        >
          <ul className="flex flex-col gap-2.5">
            {page.attention
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
                      item.count === 0
                        ? "var(--color-text-muted)"
                        : item.ruleCode === "noTestcase"
                          ? "var(--color-admin-negative)"
                          : "var(--color-admin-warn)",
                  }}
                >
                  {item.count}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <ManagedListDialog
        open={managingTopics}
        onClose={() => setManagingTopics(false)}
        items={topicList}
        usage={topicUsage}
        onAdd={addProblemTopic}
        onRename={renameProblemTopic}
        onRemove={removeProblemTopic}
        onMove={moveProblemTopic}
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
          error: { empty: t("topicManager.error.empty"), duplicate: t("topicManager.error.duplicate") },
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
        count={selected.size}
        onApply={applyLevel}
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
            toast.success(t("toast.deleted", { title: pendingDelete.title }));
          }
          setPendingDelete(null);
        }}
        title={t("confirmDeleteTitle", { title: pendingDelete?.title ?? "" })}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmDeleteBody")}
      </ConfirmDialog>
    </div>
  );
}
