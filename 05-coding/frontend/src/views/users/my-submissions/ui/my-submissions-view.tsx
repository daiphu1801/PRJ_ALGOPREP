// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0202_my_submissions).
//
// Layout follows 02-bd/screens/users/USR0202_my_submissions.md Sheet 4.4: filter toolbar (B),
// history table (C), footer + pagination (D). The stats strip (A) was removed 2026-10-18 (owner: no
// list screen carries a KPI strip). Reference mockup: 09-layoutBase/Bài đã nộp.dc.html. Filter
// state lives in local state here rather than the URL (BD calls for `q`/`verdict`/`language`/`page`
// query params, deferred: no DD yet to lock the param names against).
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import {
  fetchMySubmissionsPage,
  type LanguageFilter,
  type SubmissionListItem,
  type VerdictFilter,
} from "@/entities/submission";
import { useLocale, useT } from "@/shared/i18n";
import {
  formatDateVN,
  usePersistedPageSize,
  type DateLocale,
} from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  DataTable,
  PageHeader,
  Pagination,
  FilterBar,
  FilterMenu,
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

export function MySubmissionsView() {
  const t = useT("mySubmissions");
  const locale = useLocale() as DateLocale;
  const levels = useProblemLevels();
  const [query, setQuery] = useState("");
  const [verdict, setVerdict] = useState<VerdictFilter>("ALL");
  const [language, setLanguage] = useState<LanguageFilter>("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = usePersistedPageSize(
    "algoprep-my-submissions-page-size",
    PAGE_SIZES,
    20,
  );

  const listPage = useMemo(
    () => fetchMySubmissionsPage({ query, verdict, language, page, pageSize }),
    [query, verdict, language, page, pageSize],
  );

  function updateFilter<T>(setter: (value: T) => void, value: T) {
    setter(value);
    setPage(1);
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
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
            href={`/problems/${row.problemSlug}`}
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

  return (
    <div className="p-6">
      <PageHeader title={t("title")} description={t("subtitle")} />

      <div className="glass-card border border-[var(--color-border)] px-5 py-4">
        <FilterBar
          search={{
            label: t("filter.searchLabel"),
            placeholder: t("filter.searchPlaceholder"),
            value: query,
            maxLength: SEARCH_MAX_LENGTH,
            onChange: (next) => updateFilter(setQuery, next),
            onSubmit: announceSearch,
          }}
        >
          <FilterMenu
            label={t("filter.verdictLabel")}
            value={verdict}
            onValueChange={(value) => updateFilter(setVerdict, value)}
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
            onValueChange={(value) => updateFilter(setLanguage, value)}
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
          onPageChange={setPage}
          pageSizeOptions={PAGE_SIZES}
          pageSizeLabel={t("footer.pageSizeLabel")}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setPage(1);
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
