// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Ngân hàng bài toán.dc.html: thanh chủ đề
// (:118-125), bộ lọc (:131-153), bảng danh sách (:155-192), phân trang (:194-201), hai khối phụ
// "Bài đang làm dở" (:206-220) và "Bài tập lớp" (:222-236) ở cột phải.
// Bố cục hai cột minmax(0,1fr) 268px theo 02-bd/screens/users/USR0101_problem_list.md (Sheet 4.4).
// Dải chỉ số (:103-116) đã BỎ 2026-10-18 (owner: màn danh sách không có dải KPI).
//
// Nhánh `isSolve` của prototype KHÔNG mang theo — chốt ở RD Câu hỏi mở Q2 (đã đóng), là mã thừa
// của một hướng thiết kế cũ.
//
// Ponytail: bộ lọc KHÔNG ghi vào URL ở bản dựng này (BD Sheet 4.1 bước 2 có nói tới) — state cục
// bộ đủ để thấy hành vi chạy, đồng bộ URL/searchParams để lại cho lúc build thật với API/DD.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Shuffle } from "lucide-react";
import {
  InProgressList,
  problemLevelLabel,
  problemLevelRank,
  problemLevelTone,
  useProblemLevels,
  useProblemListPage,
  type ProblemListItem,
  type ProblemListPage,
  type SolveState,
} from "@/entities/problem";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN, usePersistedPageSize } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  Pagination,
  FilterBar,
  FilterMenu,
  QueryBoundary,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const SOLVE_STATE_VARIANT: Record<SolveState, BadgeVariant> = {
  solved: "success",
  attempted: "warn",
  todo: "neutral",
};

// 12 is the default (a multiple of the card grid's columns); the learner may pick another.
const PAGE_SIZES = [12, 24, 48] as const;

// Class assignments block: at most the 3 most recently assigned classes, no "view all" link
// (owner 2026-10-18). The mock has no assigned_at, so the first 3 groups stand in for "most recent".
const MAX_ASSIGNMENT_CLASSES = 3;

// "Bài đang làm dở" shows at most 3 items (BD Sheet 4.4); the real endpoint also caps it.
const MAX_IN_PROGRESS = 3;

const SEARCH_MAX_LENGTH = 100;

type StatusFilter = SolveState | "all";
type DifficultyFilter = string;
type SortKey = "difficulty" | "acRate";

export function ProblemListView() {
  const query = useProblemListPage();
  return (
    <QueryBoundary query={query}>
      {(page) => <ProblemList page={page} />}
    </QueryBoundary>
  );
}

function ProblemList({ page }: { page: ProblemListPage }) {
  const t = useT("problemList");
  const locale = useLocale() === "en" ? "en" : "vi";
  const router = useRouter();
  const levels = useProblemLevels();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");
  const [topicId, setTopicId] = useState<string>("all");
  const [sort, setSort] = useState<{ key: SortKey; direction: "asc" | "desc" }>(
    {
      key: "difficulty",
      direction: "asc",
    },
  );
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-problem-list-page-size",
    PAGE_SIZES,
    12,
  );

  const resetToFirstPage = () => setCurrentPage(1);

  const hasActiveFilter =
    query.trim() !== "" ||
    status !== "all" ||
    difficulty !== "all" ||
    topicId !== "all";

  // BD EVT-6: one link resets all four filters and returns to page 1.
  function clearFilters() {
    setQuery("");
    setStatus("all");
    setDifficulty("all");
    setTopicId("all");
    resetToFirstPage();
  }

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = page.items.filter(
      (problem) =>
        (status === "all" || problem.solveState === status) &&
        (difficulty === "all" || problem.difficulty === difficulty) &&
        (topicId === "all" ||
          problem.topics.some((topic) => topic.toLowerCase() === topicId)) &&
        (!needle ||
          problem.code.toLowerCase().includes(needle) ||
          problem.title.toLowerCase().includes(needle)),
    );

    const factor = sort.direction === "asc" ? 1 : -1;
    return [...rows].sort((left, right) => {
      if (sort.key === "acRate") {
        return ((left.acRate ?? -1) - (right.acRate ?? -1)) * factor;
      }
      return (
        (problemLevelRank(levels, left.difficulty) -
          problemLevelRank(levels, right.difficulty)) *
        factor
      );
    });
  }, [page.items, query, status, difficulty, topicId, sort, levels]);

  const visible = filtered.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  const assignmentGroups = page.classAssignments.slice(
    0,
    MAX_ASSIGNMENT_CLASSES,
  );

  function pickRandom() {
    const problem = filtered[Math.floor(Math.random() * filtered.length)];
    if (!problem) {
      toast.warning(t("toast.randomEmpty"));
      return;
    }
    router.push(`/problems/${problem.id}`);
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    toast.info(
      filtered.length > 0
        ? t("toast.searchResult", { count: filtered.length })
        : t("toast.searchEmpty"),
    );
  }

  const columns: DataTableColumn<ProblemListItem>[] = [
    {
      key: "solveState",
      header: t("table.colState"),
      width: "48px",
      render: (problem) => (
        <>
          <span
            aria-hidden="true"
            className="inline-block h-2 w-2 rounded-full"
            style={{
              background: `var(${
                problem.solveState === "solved"
                  ? "--color-success"
                  : problem.solveState === "attempted"
                    ? "--color-admin-warn"
                    : "--color-border"
              })`,
            }}
          />
          <span className="sr-only">{t(`state.${problem.solveState}`)}</span>
        </>
      ),
    },
    {
      key: "title",
      header: t("table.colTitle"),
      sortable: false,
      render: (problem) => (
        <div className="min-w-0">
          <Link
            href={`/problems/${problem.id}`}
            className="block truncate font-semibold hover:underline"
          >
            <span className="mr-1.5 font-mono text-xs text-[var(--color-text-muted)]">
              {problem.code}
            </span>
            {problem.title}
          </Link>
          <div className="mt-1 flex flex-wrap items-center gap-1.5">
            {/* BD Sheet 5 khu D NO 5: only the exception is labelled; "both" is the norm and stays silent. */}
            {problem.submissionModel === "stdioOnly" ? (
              <Badge variant="neutral">{t("submissionModel.stdioOnly")}</Badge>
            ) : null}
            {problem.hasSolutionReview ? (
              <Badge variant="teal">{t("table.hasReview")}</Badge>
            ) : null}
          </div>
        </div>
      ),
    },
    {
      key: "topics",
      header: t("table.colTopics"),
      width: "132px",
      render: (problem) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {problem.topics.join(", ")}
        </span>
      ),
    },
    {
      key: "difficulty",
      header: t("table.colDifficulty"),
      width: "108px",
      sortable: true,
      render: (problem) => (
        <Badge variant={problemLevelTone(levels, problem.difficulty)}>
          {problemLevelLabel(levels, problem.difficulty)}
        </Badge>
      ),
    },
    {
      key: "acRate",
      header: t("table.colAcRate"),
      width: "84px",
      align: "right",
      sortable: true,
      render: (problem) => (
        <span className="font-mono font-semibold">
          {problem.acRate === null ? "-" : `${problem.acRate}%`}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "96px",
      align: "right",
      render: (problem) => (
        <Button
          asChild
          variant="ghost"
          size="sm"
          className="border border-[var(--color-border)]"
        >
          <Link href={`/problems/${problem.id}`}>{t("table.btnSolve")}</Link>
        </Button>
      ),
    },
  ];

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
        <div className="flex gap-2">
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
          >
            <Link href="/saved">{t("summary.linkSaved")}</Link>
          </Button>
          <Button variant="cta" size="sm" onClick={pickRandom}>
            <Shuffle className="mr-1.5 h-3.5 w-3.5" />
            {t("summary.btnRandom")}
          </Button>
        </div>
      </div>

      <div
        className="mb-4 flex gap-2 overflow-x-auto pb-1"
        role="group"
        aria-label={t("topicNav.label")}
      >
        <button
          type="button"
          aria-pressed={topicId === "all"}
          onClick={() => {
            setTopicId("all");
            resetToFirstPage();
          }}
          className={`shrink-0 rounded-full border px-3 py-1.5 text-[12.5px] font-semibold ${
            topicId === "all"
              ? "border-[var(--color-primary)] text-[var(--color-primary)]"
              : "border-[var(--color-border)] text-[var(--color-text-muted)]"
          }`}
        >
          {t("filter.all")}
        </button>
        {page.topics.map((topic) => (
          <button
            key={topic.id}
            type="button"
            aria-pressed={topicId === topic.id}
            onClick={() => {
              // BD EVT-5: pressing the active topic clears the filter.
              setTopicId(topicId === topic.id ? "all" : topic.id);
              resetToFirstPage();
            }}
            className={`shrink-0 rounded-full border px-3 py-1.5 text-[12.5px] font-semibold whitespace-nowrap ${
              topicId === topic.id
                ? "border-[var(--color-primary)] text-[var(--color-primary)]"
                : "border-[var(--color-border)] text-[var(--color-text-muted)]"
            }`}
          >
            {topic.name} ({topic.solved}/{topic.total})
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:[grid-template-columns:minmax(0,1fr)_268px]">
        <Card className="min-w-0 px-[18px] py-4">
          <FilterBar
            search={{
              label: t("filter.searchLabel"),
              placeholder: t("filter.searchPlaceholder"),
              value: query,
              maxLength: SEARCH_MAX_LENGTH,
              onChange: (next) => {
                setQuery(next);
                resetToFirstPage();
              },
              onSubmit: announceSearch,
            }}
          >
            <FilterMenu
              label={t("filter.statusLabel")}
              value={status}
              onValueChange={(value) => {
                setStatus(value);
                resetToFirstPage();
              }}
              options={[
                { value: "all", label: t("filter.all") },
                { value: "solved", label: t("state.solved") },
                { value: "attempted", label: t("state.attempted") },
                { value: "todo", label: t("state.todo") },
              ]}
            />
            <FilterMenu
              label={t("filter.difficultyLabel")}
              value={difficulty}
              onValueChange={(value) => {
                setDifficulty(value);
                resetToFirstPage();
              }}
              options={[
                { value: "all", label: t("filter.all") },
                ...levels.map((level) => ({
                  value: level.key,
                  label: level.label,
                })),
              ]}
            />
          </FilterBar>

          <DataTable
            caption={t("table.caption")}
            columns={columns}
            rows={visible}
            rowKey={(problem) => problem.id}
            emptyMessage={
              <span className="flex flex-col items-center gap-2">
                {t("table.empty")}
                {hasActiveFilter ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    onClick={clearFilters}
                  >
                    {t("table.clearFilters")}
                  </Button>
                ) : null}
              </span>
            }
            minWidth={640}
            sort={{
              key: sort.key,
              direction: sort.direction,
              onSortChange: (key, direction) => {
                setSort({ key: key as SortKey, direction });
                resetToFirstPage();
              },
            }}
          />

          <Pagination
            page={currentPage}
            pageSize={pageSize}
            total={filtered.length}
            onPageChange={setCurrentPage}
            pageSizeOptions={PAGE_SIZES}
            pageSizeLabel={t("pager.pageSizeLabel")}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setCurrentPage(1);
            }}
            summary={t("pager.summary", {
              from:
                filtered.length === 0 ? 0 : (currentPage - 1) * pageSize + 1,
              to: Math.min(currentPage * pageSize, filtered.length),
              total: filtered.length,
            })}
            previousLabel={t("pager.previous")}
            nextLabel={t("pager.next")}
            showPageNumbers
            pageLabel={(value) => t("pager.goToPage", { page: value })}
          />
        </Card>

        <div className="flex flex-col gap-4">
          <Card className="px-4 py-3.5">
            <h2 className="mb-2 text-[13px] font-semibold">
              {t("inProgress.title")}
            </h2>
            <InProgressList
              items={page.inProgress}
              limit={MAX_IN_PROGRESS}
              layout="compact"
              emptyText={t("inProgress.empty")}
              footerLink={{ href: "/workspace", label: t("inProgress.viewAll") }}
            />
          </Card>

          <Card className="px-4 py-3.5">
            <h2 className="mb-2 text-[13px] font-semibold">
              {t("assignment.title")}
            </h2>
            {page.classAssignments.length === 0 ? (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">
                {t("assignment.empty")}
              </p>
            ) : (
              assignmentGroups.map((group) => (
                <div key={group.className} className="mb-3 last:mb-0">
                  <p className="mb-1.5 text-[11.5px] font-semibold text-[var(--color-text-muted)]">
                    {group.className}
                  </p>
                  <ul className="flex flex-col gap-2">
                    {group.items.map((item) => (
                      <li key={item.problemId}>
                        <Link
                          href={`/problems/${item.problemId}`}
                          className="block rounded-lg border border-[var(--color-border)] px-2.5 py-2 hover:bg-[var(--color-row-hover)]"
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span className="truncate text-[12.5px] font-semibold">
                              {item.title}
                            </span>
                            <Badge
                              variant={SOLVE_STATE_VARIANT[item.solveState]}
                            >
                              {t(`state.${item.solveState}`)}
                            </Badge>
                          </div>
                          <p className="text-[11.5px] text-[var(--color-text-muted)]">
                            {t("assignment.meta", {
                              solved: item.solvedCount,
                              total: item.totalCount,
                              date: formatDateVN(item.assignedAt, { locale }),
                            })}
                          </p>
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              ))
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
