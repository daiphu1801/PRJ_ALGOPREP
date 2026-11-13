// USR0101 on the real api (03-dd/api/F2_problem-bank.md #1 to #4, F4 #8); mock mode answers the same contract.
//
// Layout follows 09-layoutBase/Ngân hàng bài toán.dc.html: topic bar (:118-125), filters (:131-153),
// table (:155-192), paging (:194-201), and the two side blocks "Bài đang làm dở" (:206-220) and
// "Bài tập lớp" (:222-236) in the right column. Two columns, minmax(0,1fr) 268px (BD USR0101 Sheet 4.4).
// The KPI strip (:103-116) was dropped 2026-10-18 (owner: a list screen has no KPI strip). The prototype's
// `isSolve` branch is not carried over (RD open question Q2, closed).
//
// The server filters, sorts and pages the table; this view only sends `CatalogParams`. Every block loads
// on its own, so one that fails says so in place and leaves the rest alone. A figure another module
// supplies can be unknown (solve state, topic progress): it is then left out, never drawn as 0.
//
// Filters, sort and page live in the URL (?q=&status=&level=&topic=&sort=&dir=&page=), the convention of the
// instructor lists, so a reload or a shared link restores them (BD Sheet 4.1 step 2). A default value is
// left out of the URL: the default sort stays difficulty ascending (owner, 2026-11-07). The page size is
// a personal preference kept in localStorage, not in the URL.
"use client";

import { useState } from "react";
import { useIsFetching } from "@tanstack/react-query";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Shuffle } from "lucide-react";
import {
  InProgressList,
  IN_PROGRESS_LIMIT,
  problemLevelLabel,
  problemLevelTone,
  useMyClassAssignments,
  useMyInProgress,
  useProblemCatalog,
  useProblemLevels,
  useRandomProblem,
  useTopicProgress,
  type CatalogParams,
  type CatalogRow,
  type SolveState,
} from "@/entities/problem";
import { useReviewedProblemIds } from "@/entities/solution-review";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import { formatDateVN, useDebounced, usePersistedPageSize } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  ErrorState,
  Pagination,
  FilterBar,
  FilterMenu,
  QueryError,
  Skeleton,
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

// The server already cuts the class block at the 3 most recently assigned classes (owner 2026-10-18).
const SEARCH_MAX_LENGTH = 100;

type StatusFilter = SolveState | "all";
type SortKey = "difficulty" | "acRate";

const STATUS_VALUES: readonly StatusFilter[] = [
  "all",
  "solved",
  "attempted",
  "todo",
];

export function ProblemListView() {
  const t = useT("problemList");
  const tc = useT("common");
  const locale = useLocale() === "en" ? "en" : "vi";
  const apiMessage = useApiErrorMessage();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const levels = useProblemLevels();
  const topics = useTopicProgress();
  // F5 #5 (real mode): label "analysed" when the catalog row itself does not know. A failed call leaves it empty.
  const reviewedIds = useReviewedProblemIds().data;
  const levelsFetching = useIsFetching({ queryKey: ["problem", "levels"] }) > 0;
  // A value the URL carries that this screen does not know falls back to the default.
  const statusParam = searchParams.get("status");
  const status: StatusFilter = STATUS_VALUES.includes(
    statusParam as StatusFilter,
  )
    ? (statusParam as StatusFilter)
    : "all";
  const requestedLevel = searchParams.get("level");
  const requestedTopic = searchParams.get("topic");
  const difficulty = levels.some((level) => level.key === requestedLevel)
    ? requestedLevel!
    : "all";
  const topicId = topics.data?.items.some(
    (topic) => topic.id === requestedTopic,
  )
    ? requestedTopic!
    : "all";
  const sort: { key: SortKey; direction: "asc" | "desc" } = {
    key: searchParams.get("sort") === "acRate" ? "acRate" : "difficulty",
    direction: searchParams.get("dir") === "desc" ? "desc" : "asc",
  };
  const requestedPage = Number(searchParams.get("page"));
  const currentPage =
    Number.isSafeInteger(requestedPage) && requestedPage > 0
      ? requestedPage
      : 1;
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  // One replace per change; a null or empty value drops the key, so defaults never reach the URL.
  const setUrlParams = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams.toString());
    for (const [key, value] of Object.entries(patch)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    const qs = next.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };
  const setStatus = (value: StatusFilter) =>
    setUrlParams({ status: value === "all" ? null : value, page: null });
  const setDifficulty = (value: string) =>
    setUrlParams({ level: value === "all" ? null : value, page: null });
  const setTopicId = (value: string) =>
    setUrlParams({ topic: value === "all" ? null : value, page: null });
  const setCurrentPage = (value: number) =>
    setUrlParams({ page: value <= 1 ? null : String(value) });
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-problem-list-page-size",
    PAGE_SIZES,
    12,
  );

  // Typing filters live, but only a pause in typing becomes a request.
  const debouncedQuery = useDebounced((searchParams.get("q") ?? "").trim());
  const params: CatalogParams = {
    q: debouncedQuery.trim().slice(0, SEARCH_MAX_LENGTH),
    status,
    difficulty,
    topicId,
    sort: sort.key,
    dir: sort.direction,
    page: currentPage,
    pageSize,
  };
  const catalogsReady =
    (!requestedLevel || levels.length > 0 || !levelsFetching) &&
    (!requestedTopic || !topics.isLoading);
  const catalog = useProblemCatalog(params, catalogsReady);
  const assignments = useMyClassAssignments();
  const inProgress = useMyInProgress();
  const random = useRandomProblem();

  const hasActiveFilter =
    query.trim() !== "" ||
    status !== "all" ||
    difficulty !== "all" ||
    topicId !== "all";

  // BD EVT-6: one link resets all four filters and returns to page 1.
  function clearFilters() {
    setQuery("");
    setUrlParams({
      q: null,
      status: null,
      level: null,
      topic: null,
      page: null,
    });
  }

  // The server picks inside the same filter, so the pick always matches what the table is showing.
  function pickRandom() {
    random.mutate(params, {
      onSuccess: (problemId) => {
        if (problemId) router.push(`/problems/${problemId}`);
        else toast.warning(t("toast.randomEmpty"));
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    const count = catalog.data?.page.totalItems ?? 0;
    toast.info(
      count > 0 ? t("toast.searchResult", { count }) : t("toast.searchEmpty"),
    );
  }

  const columns: DataTableColumn<CatalogRow>[] = [
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
          <span className="sr-only">
            {t(`state.${problem.solveState ?? "unknown"}`)}
          </span>
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
            {/* null = F5 did not answer (or is not built): no badge rather than a guess. */}
            {(problem.hasSolutionReview ?? reviewedIds?.has(problem.id)) ? (
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
          <Link
            href={`/problems/${problem.id}`}
            aria-label={t("table.btnSolveFor", { title: problem.title })}
          >
            {t("table.btnSolve")}
          </Link>
        </Button>
      ),
    },
  ];

  const data = catalog.data;
  // A block with nothing to show is left out whole; loading and error stay visible so a failure is never
  // mistaken for "nothing here".
  const showInProgress = !inProgress.data || inProgress.data.length > 0;
  const showAssignments = !assignments.data || assignments.data.length > 0;
  const showSide = showInProgress || showAssignments;

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
          <Button
            variant="cta"
            size="sm"
            onClick={pickRandom}
            disabled={random.isPending}
          >
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
          }}
          className={`shrink-0 rounded-full border px-3 py-1.5 text-[12.5px] font-semibold ${
            topicId === "all"
              ? "border-[var(--color-primary)] text-[var(--color-primary)]"
              : "border-[var(--color-border)] text-[var(--color-text-muted)]"
          }`}
        >
          {t("filter.all")}
        </button>
        {topics.isError ? (
          <span className="self-center text-[12.5px] text-[var(--color-text-muted)]">
            {t("topicNav.failed")}
          </span>
        ) : null}
        {topics.data?.items.map((topic) => (
          <button
            key={topic.id}
            type="button"
            aria-pressed={topicId === topic.id}
            onClick={() => {
              // BD EVT-5: pressing the active topic clears the filter.
              setTopicId(topicId === topic.id ? "all" : topic.id);
            }}
            className={`shrink-0 rounded-full border px-3 py-1.5 text-[12.5px] font-semibold whitespace-nowrap ${
              topicId === topic.id
                ? "border-[var(--color-primary)] text-[var(--color-primary)]"
                : "border-[var(--color-border)] text-[var(--color-text-muted)]"
            }`}
          >
            {/* An unknown solved count leaves only the total: "0/12" would be a made-up figure. */}
            {topic.solved === null
              ? `${topic.name} (${topic.total})`
              : `${topic.name} (${topic.solved}/${topic.total})`}
          </button>
        ))}
      </div>

      <div
        className={
          showSide
            ? "grid grid-cols-1 gap-4 lg:[grid-template-columns:minmax(0,1fr)_268px]"
            : "grid grid-cols-1 gap-4"
        }
      >
        <Card className="min-w-0 px-[18px] py-4">
          <FilterBar
            search={{
              label: t("filter.searchLabel"),
              placeholder: t("filter.searchPlaceholder"),
              value: query,
              maxLength: SEARCH_MAX_LENGTH,
              onChange: (next) => {
                setQuery(next);
                setUrlParams({ q: next.trim(), page: null });
              },
              onSubmit: announceSearch,
            }}
          >
            <FilterMenu
              label={t("filter.statusLabel")}
              value={status}
              onValueChange={(value) => {
                setStatus(value);
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

          {data && !data.solveStateAvailable ? (
            <p
              role="status"
              className="mb-3 text-[12.5px] text-[var(--color-text-muted)]"
            >
              {t("degraded.solveState")}
            </p>
          ) : null}

          {catalog.isError ? (
            <QueryError query={catalog} />
          ) : !data ? (
            <div role="status" aria-label={tc("loading")} className="space-y-3">
              <Skeleton className="h-64 w-full" />
            </div>
          ) : (
            <>
              <DataTable
                caption={t("table.caption")}
                columns={columns}
                rows={data.items}
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
                    setUrlParams({
                      sort: key === "difficulty" ? null : key,
                      dir: direction === "asc" ? null : direction,
                      page: null,
                    });
                  },
                }}
              />

              <Pagination
                page={data.page.number}
                pageSize={pageSize}
                total={data.page.totalItems}
                onPageChange={setCurrentPage}
                pageSizeOptions={PAGE_SIZES}
                pageSizeLabel={t("pager.pageSizeLabel")}
                onPageSizeChange={(size) => {
                  setPageSize(size);
                  setCurrentPage(1);
                }}
                summary={t("pager.summary", {
                  from:
                    data.page.totalItems === 0
                      ? 0
                      : (data.page.number - 1) * pageSize + 1,
                  to: Math.min(
                    data.page.number * pageSize,
                    data.page.totalItems,
                  ),
                  total: data.page.totalItems,
                })}
                previousLabel={t("pager.previous")}
                nextLabel={t("pager.next")}
                showPageNumbers
                pageLabel={(value) => t("pager.goToPage", { page: value })}
              />
            </>
          )}
        </Card>

        {showSide ? (
          <div className="flex flex-col gap-4">
            {showInProgress ? (
              <Card className="px-4 py-3.5">
                <h2 className="mb-2 text-[13px] font-semibold">
                  {t("inProgress.title")}
                </h2>
                {inProgress.isError ? (
                  <ErrorState>{t("blockFailed")}</ErrorState>
                ) : !inProgress.data ? (
                  <Skeleton className="h-16 w-full" />
                ) : (
                  <InProgressList
                    items={inProgress.data}
                    limit={IN_PROGRESS_LIMIT}
                    layout="compact"
                    emptyText={t("inProgress.empty")}
                    footerLink={{
                      href: "/workspace",
                      label: t("inProgress.viewAll"),
                    }}
                  />
                )}
              </Card>
            ) : null}

            {showAssignments ? (
              <Card className="px-4 py-3.5">
                <h2 className="mb-2 text-[13px] font-semibold">
                  {t("assignment.title")}
                </h2>
                {assignments.isError ? (
                  <ErrorState>{t("blockFailed")}</ErrorState>
                ) : !assignments.data ? (
                  <Skeleton className="h-16 w-full" />
                ) : (
                  assignments.data.map((group) => (
                    <div key={group.classId} className="mb-3 last:mb-0">
                      <p className="mb-1.5 text-[11.5px] font-semibold text-[var(--color-text-muted)]">
                        {group.className}
                      </p>
                      <ul className="flex flex-col gap-2">
                        {group.items.map((item) => (
                          <li key={`${item.problemId}-${item.assignedAt}`}>
                            <Link
                              href={`/problems/${item.problemId}`}
                              className="block rounded-lg border border-[var(--color-border)] px-2.5 py-2 hover:bg-[var(--color-row-hover)]"
                            >
                              <div className="flex items-center justify-between gap-2">
                                <span className="truncate text-[12.5px] font-semibold">
                                  {item.title}
                                </span>
                                {item.solveState ? (
                                  <Badge
                                    variant={
                                      SOLVE_STATE_VARIANT[item.solveState]
                                    }
                                  >
                                    {t(`state.${item.solveState}`)}
                                  </Badge>
                                ) : null}
                              </div>
                              <p className="text-[11.5px] text-[var(--color-text-muted)]">
                                {t("assignment.meta", {
                                  date: formatDateVN(item.assignedAt, {
                                    locale,
                                  }),
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
            ) : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}
