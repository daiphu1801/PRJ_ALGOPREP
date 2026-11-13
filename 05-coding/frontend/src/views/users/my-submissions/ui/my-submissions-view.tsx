// Layout follows 02-bd/screens/users/USR0202_my_submissions.md Sheet 4.4: filter toolbar (B),
// history table (C), footer + pagination (D). The stats strip (A) was removed 2026-10-18 (owner: no
// list screen carries a KPI strip). Filters and pagination follow the URL contract; search waits400ms.
"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { isMockMode } from "@/shared/api";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import {
  useMySubmissionsPage,
  type LanguageFilter,
  type SubmissionListItem,
  type SubmissionListPage,
  type VerdictFilter,
} from "@/entities/submission";
import { useLocale, useT } from "@/shared/i18n";
import {
  formatDateVN,
  usePersistedPageSize,
  type DateLocale,
} from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { useSubmissionFilters } from "../model/use-submission-filters";
import { normalizeSubmissionQuery } from "../model/submission-url";
import {
  Badge,
  Button,
  DataTable,
  Pagination,
  FilterBar,
  FilterMenu,
  QueryError,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const VERDICT_VARIANT: Record<string, BadgeVariant> = {
  ACCEPTED: "success",
  WRONG_ANSWER: "negative",
  TIME_LIMIT_EXCEEDED: "warn",
  SYSTEM_ERROR: "warn",
  RUNTIME_ERROR: "negative",
  COMPILE_ERROR: "warn",
};

// 20 is the default; the picker lets the learner choose, remembered per browser.
const PAGE_SIZES = [10, 20, 50] as const;
// BD USR0202 Sheet 9 NO 3: the search box takes at most 100 characters.
const SEARCH_MAX_LENGTH = 100;

const EMPTY_PAGE: SubmissionListPage = {
  items: [],
  currentPage: 1,
  pageSize: 20,
  totalItems: 0,
};

export function MySubmissionsView() {
  const t = useT("mySubmissions");
  const locale = useLocale() as DateLocale;
  const levels = useProblemLevels();
  const { filters, query, setQuery, navigate } = useSubmissionFilters();
  const { verdict, language, page } = filters;
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-my-submissions-page-size",
    PAGE_SIZES,
    20,
  );

  const listQuery = useMySubmissionsPage({
    query: filters.query,
    verdict,
    language,
    page,
    pageSize,
  });
  // Keep the previous page on screen while a filter change refetches, so the toolbar and the
  // search box never flash an empty table.
  const [lastPage, setLastPage] = useState<SubmissionListPage>(EMPTY_PAGE);
  if (listQuery.data && listQuery.data !== lastPage) {
    setLastPage(listQuery.data);
  }
  const listPage = listQuery.data ?? lastPage;
  const pendingAnnouncement = useRef<string | null>(null);
  useEffect(() => {
    if (
      pendingAnnouncement.current !== filters.query ||
      listQuery.isFetching ||
      !listQuery.data
    )
      return;
    pendingAnnouncement.current = null;
    toast.info(
      listQuery.data.totalItems > 0
        ? t("toast.searchResult", { count: listQuery.data.totalItems })
        : t("toast.searchEmpty"),
    );
  }, [filters.query, listQuery.data, listQuery.isFetching, t]);

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    const normalized = normalizeSubmissionQuery(query);
    if (
      normalized !== filters.query ||
      listQuery.isFetching ||
      !listQuery.data
    ) {
      pendingAnnouncement.current = normalized;
      navigate({ query: normalized, page: 1 });
      return;
    }
    toast.info(
      listPage.totalItems > 0
        ? t("toast.searchResult", { count: listPage.totalItems })
        : t("toast.searchEmpty"),
    );
  }

  const columns: DataTableColumn<SubmissionListItem>[] = [
    {
      key: "submittedAt",
      header: t("table.col.submittedAt"),
      width: "160px",
      render: (row) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {formatDateVN(row.submittedAt, { locale, withTime: true })}
        </span>
      ),
    },
    {
      key: "problem",
      header: t("table.col.problem"),
      render: (row) => (
        <div className="min-w-0">
          <Link
            href={
              row.problemWithdrawn
                ? "#"
                : `/problems/${row.problemId || row.problemSlug}`
            }
            aria-disabled={row.problemWithdrawn}
            onClick={(event) => {
              if (row.problemWithdrawn) event.preventDefault();
            }}
            className="font-semibold hover:underline"
          >
            {row.problemCode} · {row.problemTitle}
          </Link>
          <span className="mt-1 flex">
            <Badge variant={problemLevelTone(levels, row.difficulty)}>
              {problemLevelLabel(levels, row.difficulty)}
            </Badge>
          </span>
        </div>
      ),
    },
    {
      key: "language",
      header: t("table.col.language"),
      width: "96px",
      render: (row) => (
        <Badge variant="neutral">{t(`language.${row.language}`)}</Badge>
      ),
    },
    {
      key: "verdict",
      header: t("table.col.verdict"),
      width: "140px",
      render: (row) => (
        <Badge variant={VERDICT_VARIANT[row.status] ?? "neutral"}>
          {t(`verdict.${row.status}`)}
        </Badge>
      ),
    },
    {
      key: "testcases",
      header: t("table.col.testcases"),
      width: "88px",
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {row.totalCount != null
            ? `${row.passedCount}/${row.totalCount}`
            : "-"}
        </span>
      ),
    },
    {
      key: "runtime",
      header: t("table.col.runtime"),
      width: "88px",
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {row.runtimeMs != null ? `${row.runtimeMs} ms` : "-"}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "180px",
      align: "right",
      render: (row) => (
        <span className="flex justify-end gap-1.5">
          <Button
            variant="ghost"
            size="sm"
            asChild
            className="border border-[var(--color-border)]"
          >
            <Link href={`/submissions/${row.id}`}>{t("table.btnResult")}</Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={row.status !== "ACCEPTED"}
            title={
              row.status === "ACCEPTED" ? undefined : t("table.needAccepted")
            }
            asChild={row.status === "ACCEPTED"}
            className="border border-[var(--color-border)]"
          >
            {row.status === "ACCEPTED" ? (
              <Link href={`/submissions/${row.id}/review`}>
                {t("table.btnReview")}
              </Link>
            ) : (
              <span>{t("table.btnReview")}</span>
            )}
          </Button>
        </span>
      ),
    },
  ];

  if (listQuery.isError) {
    return (
      <div className="p-6">
        <QueryError query={listQuery} />
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-4">
        <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("subtitle")}
        </p>
      </div>

      <div className="glass-card border border-[var(--color-border)] px-5 py-4">
        <FilterBar
          search={{
            label: t("filter.searchLabel"),
            placeholder: t("filter.searchPlaceholder"),
            value: query,
            maxLength: SEARCH_MAX_LENGTH,
            onChange: setQuery,
            onSubmit: announceSearch,
          }}
        >
          <FilterMenu
            label={t("filter.verdictLabel")}
            value={verdict}
            onValueChange={(value) =>
              navigate({ verdict: value as VerdictFilter, page: 1 })
            }
            options={[
              { value: "ALL", label: t("filter.all") },
              { value: "ACCEPTED", label: t("verdict.ACCEPTED") },
              { value: "WRONG_ANSWER", label: t("verdict.WRONG_ANSWER") },
              {
                value: "TIME_LIMIT_EXCEEDED",
                label: t("verdict.TIME_LIMIT_EXCEEDED"),
              },
              { value: "COMPILE_ERROR", label: t("verdict.COMPILE_ERROR") },
              { value: "RUNTIME_ERROR", label: t("verdict.RUNTIME_ERROR") },
            ]}
          />
          <FilterMenu
            label={t("filter.languageLabel")}
            value={language}
            onValueChange={(value) =>
              navigate({ language: value as LanguageFilter, page: 1 })
            }
            options={[
              { value: "ALL", label: t("filter.all") },
              { value: "PYTHON", label: t("language.PYTHON") },
              { value: "JAVA", label: t("language.JAVA") },
              { value: "CPP", label: t("language.CPP") },
            ]}
          />
        </FilterBar>

        <DataTable
          caption={t("table.caption")}
          columns={columns}
          rows={listPage.items}
          rowKey={(row) => row.id}
          emptyMessage={t("table.empty")}
          minWidth={820}
        />

        <Pagination
          page={listPage.currentPage}
          pageSize={listPage.pageSize}
          total={listPage.totalItems}
          onPageChange={(page) => navigate({ page })}
          pageSizeOptions={isMockMode() ? PAGE_SIZES : undefined}
          pageSizeLabel={t("footer.pageSizeLabel")}
          onPageSizeChange={(size) => {
            setPageSize(size);
            navigate({ page: 1 });
          }}
          summary={t("footer.summary", {
            from:
              listPage.totalItems === 0
                ? 0
                : (listPage.currentPage - 1) * listPage.pageSize + 1,
            to: Math.min(
              listPage.currentPage * listPage.pageSize,
              listPage.totalItems,
            ),
            total: listPage.totalItems,
          })}
          previousLabel={t("footer.previous")}
          nextLabel={t("footer.next")}
          showPageNumbers
          pageLabel={(pageNumber) =>
            t("footer.pageLabel", { page: pageNumber })
          }
        />
      </div>
    </div>
  );
}
