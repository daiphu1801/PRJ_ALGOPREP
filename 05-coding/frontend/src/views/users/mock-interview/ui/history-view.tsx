// The learner's whole mock-interview history as an ordinary data grid: search, four menu filters, sortable date and
// score columns, paging. Everything is filtered, sorted and paged by the server (F5 section 31A); a row opens the
// session in detail (USR0302 history detail). Filters live in the screen state, not in the URL.
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, ArrowRight } from "lucide-react";
import {
  DEFAULT_HISTORY_QUERY,
  INTERVIEWER_LEVELS,
  useInterviewHistory,
  type HistoryScoreBand,
  type InterviewHistoryItem,
  type InterviewHistoryQuery,
} from "@/entities/mock-interview";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN, type DateLocale } from "@/shared/lib";
import {
  Badge,
  DataTable,
  FilterBar,
  FilterMenu,
  IconAction,
  Pagination,
  QueryError,
  type DataTableColumn,
} from "@/shared/ui";
import {
  SCORE_LEVELS,
  SCORE_LEVEL_COLOR,
  scoreLevel,
} from "../model/score-level";

const PAGE_SIZES = [10, 20, 50] as const;
/** The server cuts a longer search; the box stops at the same length. */
const SEARCH_MAX_LENGTH = 100;
/** The search reaches the server only after the learner stops typing. */
const SEARCH_DEBOUNCE_MS = 400;

export function MockInterviewHistoryView() {
  const t = useT("mockInterview");
  const locale = useLocale() as DateLocale;
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState<InterviewHistoryQuery>(
    DEFAULT_HISTORY_QUERY,
  );
  useEffect(() => {
    const timer = window.setTimeout(
      () =>
        setQuery((c) => (c.q === search ? c : { ...c, q: search, page: 1 })),
      SEARCH_DEBOUNCE_MS,
    );
    return () => window.clearTimeout(timer);
  }, [search]);
  const list = useInterviewHistory(query);
  const data = list.data;

  /** Any filter change goes back to the first page. */
  const change = (patch: Partial<InterviewHistoryQuery>) =>
    setQuery((current) => ({ ...current, ...patch, page: 1 }));

  const columns: DataTableColumn<InterviewHistoryItem>[] = [
    {
      key: "startedAt",
      header: t("history.col.date"),
      width: "15%",
      sortable: true,
      render: (row) => (
        <Link
          href={`/mock-interview/history/${row.sessionId}`}
          className="font-semibold whitespace-nowrap hover:underline"
        >
          {formatDateVN(row.startedAt, { locale, withTime: true })}
        </Link>
      ),
    },
    {
      key: "source",
      header: t("history.col.source"),
      render: (row) => (
        // w-0 + min-w-full: a long question is cut inside the column instead of widening the table.
        <div className="w-0 min-w-full">
          <Badge variant="blue">{t(`tab.${row.entryType}`)}</Badge>
          {row.questionText || row.topics.length > 0 ? (
            <p
              className="mt-1 truncate text-[12.5px] text-[var(--color-text-muted)]"
              title={row.questionText ?? row.topics.join(", ")}
            >
              {row.questionText ?? row.topics.join(", ")}
            </p>
          ) : null}
        </div>
      ),
    },
    {
      key: "level",
      header: t("history.col.level"),
      width: "10%",
      render: (row) => (row.level ? t(`level.${row.level}`) : "-"),
    },
    {
      key: "turns",
      header: t("history.col.turns"),
      width: "9%",
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {row.maxTurns === null
            ? row.turnCount
            : `${row.turnCount}/${row.maxTurns}`}
        </span>
      ),
    },
    {
      key: "score",
      header: t("history.col.score"),
      width: "16%",
      align: "right",
      sortable: true,
      render: (row) => <ScoreCell score={row.overallScore} />,
    },
    {
      key: "status",
      header: t("history.col.status"),
      width: "12%",
      render: (row) => (
        <Badge variant={row.status === "completed" ? "success" : "neutral"}>
          {t(`history.status.${row.status}`)}
        </Badge>
      ),
    },
    {
      key: "actions",
      header: <span className="sr-only">{t("history.viewDetail")}</span>,
      width: "48px",
      align: "right",
      render: (row) => (
        <Link
          href={`/mock-interview/history/${row.sessionId}`}
          aria-label={t("history.viewDetail")}
          className="inline-flex text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
        >
          <ArrowRight size={16} aria-hidden />
        </Link>
      ),
    },
  ];

  if (list.isError && !data) {
    return (
      <div className="p-6">
        <QueryError query={list} />
      </div>
    );
  }

  const page = data?.page;
  const from =
    !page || page.totalItems === 0 ? 0 : (page.number - 1) * page.size + 1;

  return (
    <section className="p-6">
      <div className="mb-4 flex items-start gap-3">
        <IconAction
          icon={ArrowLeft}
          label={t("history.back")}
          href="/mock-interview"
        />
        <div>
          <h1 className="text-lg font-bold tracking-tight">
            {t("history.title")}
          </h1>
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("history.subtitle")}
          </p>
        </div>
      </div>

      <div className="glass-card border border-[var(--color-border)] px-5 py-4">
        <FilterBar
          search={{
            label: t("history.filter.searchLabel"),
            placeholder: t("history.filter.searchPlaceholder"),
            value: search,
            maxLength: SEARCH_MAX_LENGTH,
            onChange: setSearch,
          }}
        >
          <FilterMenu
            label={t("history.filter.source")}
            value={query.entryType}
            onValueChange={(entryType) => change({ entryType })}
            options={[
              { value: "all" as const, label: t("history.filter.all") },
              { value: "submission" as const, label: t("tab.submission") },
              { value: "bank" as const, label: t("tab.bank") },
              { value: "custom" as const, label: t("tab.custom") },
            ]}
          />
          <FilterMenu
            label={t("history.filter.status")}
            value={query.status}
            onValueChange={(status) => change({ status })}
            options={[
              { value: "all" as const, label: t("history.filter.all") },
              {
                value: "completed" as const,
                label: t("history.status.completed"),
              },
              {
                value: "abandoned" as const,
                label: t("history.status.abandoned"),
              },
            ]}
          />
          <FilterMenu
            label={t("history.filter.band")}
            value={query.scoreBand}
            onValueChange={(scoreBand) => change({ scoreBand })}
            options={[
              { value: "all" as const, label: t("history.filter.all") },
              ...SCORE_LEVELS.map((level) => ({
                value: level as HistoryScoreBand,
                label: `${t(`scoreLevel.${level}`)} (${t(`legend.${level}`)})`,
              })),
            ]}
          />
          <FilterMenu
            label={t("history.filter.level")}
            value={query.level}
            onValueChange={(level) => change({ level })}
            options={[
              { value: "all" as const, label: t("history.filter.all") },
              ...INTERVIEWER_LEVELS.map((level) => ({
                value: level,
                label: t(`level.${level}`),
              })),
            ]}
          />
        </FilterBar>

        <DataTable
          caption={t("history.title")}
          columns={columns}
          rows={data?.items ?? []}
          rowKey={(row) => row.sessionId}
          status={list.isPending ? "loading" : "ready"}
          loadingRowCount={query.size}
          emptyMessage={t("history.empty")}
          minWidth={860}
          sort={{
            key: query.sort,
            direction: query.order,
            onSortChange: (key, direction) =>
              change({
                sort: key === "score" ? "score" : "startedAt",
                order: direction,
              }),
          }}
        />

        <Pagination
          page={page?.number ?? 1}
          pageSize={page?.size ?? query.size}
          total={page?.totalItems ?? 0}
          onPageChange={(next) => setQuery((c) => ({ ...c, page: next }))}
          pageSizeOptions={PAGE_SIZES}
          pageSizeLabel={t("history.footer.pageSize")}
          onPageSizeChange={(size) => change({ size })}
          summary={t("history.footer.summary", {
            from,
            to: page ? Math.min(page.number * page.size, page.totalItems) : 0,
            total: page?.totalItems ?? 0,
          })}
          previousLabel={t("history.footer.previous")}
          nextLabel={t("history.footer.next")}
          showPageNumbers
          pageLabel={(n) => t("history.footer.pageLabel", { page: n })}
        />
      </div>
    </section>
  );
}

/** The overall score with its band name, coloured like the scorecard; a dash for a session with no score. */
export function ScoreCell({ score }: { score: number | null }) {
  const t = useT("mockInterview");
  if (score === null) {
    return <span className="text-[var(--color-text-muted)]">-</span>;
  }
  const level = scoreLevel(score);
  return (
    <span
      className="inline-flex items-center gap-2 font-mono font-bold whitespace-nowrap"
      style={{ color: SCORE_LEVEL_COLOR[level] }}
    >
      {score} / 10
      <span className="font-sans text-[11.5px] font-semibold">
        {t(`scoreLevel.${level}`)}
      </span>
    </span>
  );
}
