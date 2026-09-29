// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Ngân hàng bài toán.dc.html: dải chỉ số (:103-116), thanh chủ đề
// (:118-125), bộ lọc (:131-153), bảng danh sách (:155-192), phân trang (:194-201), hai khối phụ
// "Bài đang làm dở" (:206-220) và "Bài tập lớp" (:222-236) ở cột phải.
// Bố cục hai cột minmax(0,1fr) 268px theo 02-bd/screens/users/USR0101_problem_list.md:262-264.
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
import { Search, Shuffle } from "lucide-react";
import {
  fetchProblemListPage,
  type Difficulty,
  type ProblemListItem,
  type SolveState,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
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

const SOLVE_STATE_VARIANT: Record<SolveState, BadgeVariant> = {
  solved: "success",
  attempted: "warn",
  todo: "neutral",
};

const PAGE_SIZE = 12;

type StatusFilter = SolveState | "all";
type DifficultyFilter = Difficulty | "all";
type SortKey = "difficulty" | "acRate";

export function ProblemListView() {
  const t = useT("problemList");
  const router = useRouter();
  const [page] = useState(fetchProblemListPage);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");
  const [topicId, setTopicId] = useState<string>("all");
  const [sort, setSort] = useState<{ key: SortKey; direction: "asc" | "desc" }>({
    key: "difficulty",
    direction: "asc",
  });
  const [currentPage, setCurrentPage] = useState(1);

  const resetToFirstPage = () => setCurrentPage(1);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = page.items.filter(
      (problem) =>
        (status === "all" || problem.solveState === status) &&
        (difficulty === "all" || problem.difficulty === difficulty) &&
        (topicId === "all" || problem.topics.some((topic) => topic.toLowerCase() === topicId)) &&
        (!needle ||
          problem.code.toLowerCase().includes(needle) ||
          problem.title.toLowerCase().includes(needle)),
    );

    const factor = sort.direction === "asc" ? 1 : -1;
    return [...rows].sort((left, right) => {
      if (sort.key === "acRate") {
        return ((left.acRate ?? -1) - (right.acRate ?? -1)) * factor;
      }
      const rank = { easy: 0, medium: 1, hard: 2 } as const;
      return (rank[left.difficulty] - rank[right.difficulty]) * factor;
    });
  }, [page.items, query, status, difficulty, topicId, sort]);

  const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function pickRandom() {
    if (filtered.length === 0) return;
    const problem = filtered[Math.floor(Math.random() * filtered.length)];
    if (!problem) return;
    router.push(`/problems/${problem.id}`);
  }

  const columns: DataTableColumn<ProblemListItem>[] = [
    {
      key: "solveState",
      header: t("table.colState"),
      width: "48px",
      render: (problem) => (
        <span
          aria-label={t(`state.${problem.solveState}`)}
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
      ),
    },
    {
      key: "title",
      header: t("table.colTitle"),
      sortable: false,
      render: (problem) => (
        <div className="min-w-0">
          <Link href={`/problems/${problem.id}`} className="block truncate font-semibold hover:underline">
            <span className="mr-1.5 font-mono text-xs text-[var(--color-text-muted)]">{problem.code}</span>
            {problem.title}
          </Link>
          <div className="mt-1 flex flex-wrap items-center gap-1.5">
            <Badge variant={problem.submissionModel === "both" ? "cyan" : "neutral"}>
              {t(`submissionModel.${problem.submissionModel}`)}
            </Badge>
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
        <Badge variant={DIFFICULTY_VARIANT[problem.difficulty]}>{t(`difficulty.${problem.difficulty}`)}</Badge>
      ),
    },
    {
      key: "acRate",
      header: t("table.colAcRate"),
      width: "84px",
      align: "right",
      sortable: true,
      render: (problem) => (
        <span className="font-mono font-semibold">{problem.acRate === null ? "-" : `${problem.acRate}%`}</span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "96px",
      align: "right",
      render: (problem) => (
        <Button asChild variant="ghost" size="sm" className="border border-[var(--color-border)]">
          <Link href={`/problems/${problem.id}`}>{t("table.btnSolve")}</Link>
        </Button>
      ),
    },
  ];

  return (
    <div className="p-6">
      <PageHeader
        title={t("title")}
        actions={
          <div className="flex gap-2">
            <Button asChild variant="ghost" size="sm" className="border border-[var(--color-border)]">
              <Link href="/saved">{t("summary.linkSaved")}</Link>
            </Button>
            <Button variant="cta" size="sm" onClick={pickRandom}>
              <Shuffle className="mr-1.5 h-3.5 w-3.5" />
              {t("summary.btnRandom")}
            </Button>
          </div>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(160px,1fr))]">
        <StatCard label={t("summary.solvedTotal")} value={`${page.summary.solvedTotal.solved} / ${page.summary.solvedTotal.total}`} />
        <StatCard label={t("difficulty.easy")} value={`${page.summary.solvedEasy.solved} / ${page.summary.solvedEasy.total}`} />
        <StatCard label={t("difficulty.medium")} value={`${page.summary.solvedMedium.solved} / ${page.summary.solvedMedium.total}`} />
        <StatCard label={t("difficulty.hard")} value={`${page.summary.solvedHard.solved} / ${page.summary.solvedHard.total}`} />
      </div>

      <div className="mb-4 flex gap-2 overflow-x-auto pb-1" role="group" aria-label={t("topicNav.label")}>
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
              setTopicId(topic.id);
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

      <div className="grid gap-4 [grid-template-columns:minmax(0,1fr)_268px]">
        <Card className="min-w-0 px-[18px] py-4">
          <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
            <TextField
              label={t("filter.searchLabel")}
              hideLabel
              leadingIcon={<Search className="h-3.5 w-3.5" />}
              placeholder={t("filter.searchPlaceholder")}
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                resetToFirstPage();
              }}
              wrapperClassName="min-w-[220px] flex-1"
            />
            <SegmentedTabs
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
            <SegmentedTabs
              label={t("filter.difficultyLabel")}
              value={difficulty}
              onValueChange={(value) => {
                setDifficulty(value);
                resetToFirstPage();
              }}
              options={[
                { value: "all", label: t("filter.all") },
                { value: "easy", label: t("difficulty.easy") },
                { value: "medium", label: t("difficulty.medium") },
                { value: "hard", label: t("difficulty.hard") },
              ]}
            />
          </div>

          <DataTable
            caption={t("table.caption")}
            columns={columns}
            rows={visible}
            rowKey={(problem) => problem.id}
            emptyMessage={t("table.empty")}
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
            pageSize={PAGE_SIZE}
            total={filtered.length}
            onPageChange={setCurrentPage}
            summary={t("pager.summary", {
              from: filtered.length === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1,
              to: Math.min(currentPage * PAGE_SIZE, filtered.length),
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
            <h2 className="mb-2 text-[13px] font-semibold">{t("inProgress.title")}</h2>
            {page.inProgress.length === 0 ? (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("inProgress.empty")}</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {page.inProgress.map((item) => (
                  <li key={item.problemId}>
                    <Link
                      href={`/problems/${item.problemId}`}
                      className="block rounded-lg border border-[var(--color-border)] px-2.5 py-2 hover:bg-[var(--color-row-hover)]"
                    >
                      <p className="truncate text-[12.5px] font-semibold">{item.title}</p>
                      <p className="text-[11.5px] text-[var(--color-text-muted)]">
                        {t(`language.${item.language}`)} · {item.lastVerdictLabel}
                      </p>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card className="px-4 py-3.5">
            <h2 className="mb-2 text-[13px] font-semibold">{t("assignment.title")}</h2>
            {page.classAssignments.length === 0 ? (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("assignment.empty")}</p>
            ) : (
              page.classAssignments.map((group) => (
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
                            <span className="truncate text-[12.5px] font-semibold">{item.title}</span>
                            <Badge variant={SOLVE_STATE_VARIANT[item.solveState]}>
                              {t(`state.${item.solveState}`)}
                            </Badge>
                          </div>
                          <p className="text-[11.5px] text-[var(--color-text-muted)]">{item.metaLabel}</p>
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
