// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Quản lý bài tập.dc.html: sticky header with the "Bài tập
// mới" CTA (:149-159), stat cards (:161-173), then the problem table card with filters, a bulk
// action bar, a sortable table and numbered paging (:175-250).
//
// Shared screen: A2 and A3 both use it, mounted at /instructor and /admin
// (DEC-2026-0825-shared-content-authoring-screens). This build wires the /admin route only —
// /instructor gets its own route when the instructor area is built.
//
// TWO lifecycle states, not three. DEC-2026-0830-problem-lifecycle-two-states drops "Đã ẩn"; the
// mockup still has it on row #987 and inside a bulk action labelled "Xuất bản / ẩn". The filter
// therefore offers draft/published only, and the bulk action is "Xuất bản".
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import {
  fetchAdminProblemPage,
  type AdminProblem,
  type Difficulty,
  type ProblemSortKey,
  type ProblemStatus,
} from "@/entities/admin-problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  Pagination,
  SegmentedTabs,
  StatCard,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const DIFFICULTY_VARIANT: Record<Difficulty, BadgeVariant> = {
  easy: "success",
  medium: "warn",
  hard: "negative",
};

const STATUS_COLOR_VAR: Record<ProblemStatus, string> = {
  published: "--color-success",
  draft: "--color-admin-warn",
};

const PAGE_SIZE = 8;

type DifficultyFilter = Difficulty | "all";
type StatusFilter = ProblemStatus | "all";

export function ProblemManagementView() {
  const t = useT("problemManagement");
  const [page] = useState(fetchAdminProblemPage);
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
  const [deletedCodes, setDeletedCodes] = useState<ReadonlySet<string>>(new Set());

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = page.problems.filter(
      (problem) =>
        !deletedCodes.has(problem.code) &&
        (difficulty === "all" || problem.difficulty === difficulty) &&
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
          const rank = { easy: 0, medium: 1, hard: 2 } as const;
          return (rank[left.difficulty] - rank[right.difficulty]) * factor;
        }
        case "status":
          return left.status.localeCompare(right.status) * factor;
        case "topic":
          return left.topic.localeCompare(right.topic, "vi") * factor;
        default:
          return left.title.localeCompare(right.title, "vi") * factor;
      }
    });
  }, [page.problems, deletedCodes, query, difficulty, status, sort]);

  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

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
          href={`/admin/problems/${problem.code.replace("#", "")}`}
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
          {problem.topic}
        </span>
      ),
    },
    {
      key: "difficulty",
      header: t("columnDifficulty"),
      width: "108px",
      sortable: true,
      render: (problem) => (
        <Badge variant={DIFFICULTY_VARIANT[problem.difficulty]}>
          {t(`difficulty.${problem.difficulty}`)}
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
          style={{ color: `var(${STATUS_COLOR_VAR[problem.status]})` }}
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
        <span className="font-mono text-xs text-[var(--color-text-subtle)]">
          {t("testcasesAndEdit", { count: problem.testcaseCount, edited: problem.editedLabel })}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "64px",
      align: "right",
      render: (problem) => (
        <span className="flex justify-end gap-1">
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)] px-2"
          >
            <Link
              href={`/admin/problems/${problem.code.replace("#", "")}`}
              aria-label={t("editProblem", { title: problem.title })}
            >
              {t("edit")}
            </Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-label={t("deleteProblem", { title: problem.title })}
            onClick={() => setPendingDelete(problem)}
            className="border border-[var(--color-border)] px-2 text-[var(--color-admin-negative)]"
          >
            {t("delete")}
          </Button>
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
          <Button asChild variant="cta" size="sm">
            <Link href="/admin/problems/new">{t("newProblem")}</Link>
          </Button>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        {page.stats.map((stat) => (
          <StatCard
            key={stat.key}
            label={t(`stat.${stat.key}.label`)}
            value={stat.value}
            delta={<span style={{ color: `var(${stat.deltaColorVar})` }}>{stat.delta}</span>}
            meta={t(`stat.${stat.key}.meta`)}
          />
        ))}
      </div>

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
            label={t("difficultyFilterLabel")}
            value={difficulty}
            onValueChange={(value) => {
              setDifficulty(value);
              setCurrentPage(1);
            }}
            options={[
              { value: "all", label: t("filterAll") },
              { value: "easy", label: t("difficulty.easy") },
              { value: "medium", label: t("difficulty.medium") },
              { value: "hard", label: t("difficulty.hard") },
            ]}
          />
          <SegmentedTabs
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
          <span className="ml-auto text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
            {t("resultCount", { shown: filtered.length, total: page.problems.length })}
          </span>
        </div>

        {selected.size > 0 ? (
          <div className="mb-3 flex flex-wrap items-center gap-2.5 rounded-2xl border border-[var(--admin-active-border)] bg-[image:var(--color-row-selected)] px-3.5 py-2.5">
            <span className="text-[13px] font-semibold">
              {t("selectionLabel", { count: selected.size })}
            </span>
            <span className="ml-auto flex flex-wrap gap-2">
              {/* "Xuất bản", not the mockup's "Xuất bản / ẩn": hiding is no longer a state. */}
              {(["publish", "changeDifficulty", "assignTopic", "duplicate", "exportCsv"] as const).map(
                (action) => (
                  <Button
                    key={action}
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                  >
                    {t(`bulk.${action}`)}
                  </Button>
                ),
              )}
              <Button
                variant="ghost"
                size="sm"
                className="text-[var(--color-admin-negative)]"
                onClick={() => setSelected(new Set())}
              >
                {t("bulk.delete")}
              </Button>
            </span>
          </div>
        ) : null}

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

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete) {
            setDeletedCodes((previous) => new Set(previous).add(pendingDelete.code));
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
