// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0202_my_submissions).
//
// Layout follows 02-bd/screens/users/USR0202_my_submissions.md Sheet 4.4: stats strip (A), filter
// toolbar (B), history table (C), footer + pagination (D). Reference mockup:
// 09-layoutBase/Bài đã nộp.dc.html.
//
// Stats are computed from raw counters only (`total_submissions`, `accepted_count`,
// `first_try_accepted_count`) — there is no `acceptedRate` column, per the BD V0.2 fix note. Filter
// state lives in local state here rather than the URL (BD calls for `q`/`verdict`/`language`/`page`
// query params, deferred: no DD yet to lock the param names against).
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import {
  fetchMySubmissionsPage,
  fetchMySubmissionStats,
  type LanguageFilter,
  type SubmissionListItem,
  type VerdictFilter,
} from "@/entities/submission";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  DataTable,
  PageHeader,
  Pagination,
  SegmentedTabs,
  StatCard,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const VERDICT_VARIANT: Record<string, BadgeVariant> = {
  ACCEPTED: "success",
  WRONG_ANSWER: "negative",
  TIME_LIMIT_EXCEEDED: "warn",
  MEMORY_LIMIT_EXCEEDED: "warn",
  RUNTIME_ERROR: "negative",
  COMPILE_ERROR: "warn",
};

const PAGE_SIZE = 20;

export function MySubmissionsView() {
  const t = useT("mySubmissions");
  const [stats] = useState(fetchMySubmissionStats);
  const [query, setQuery] = useState("");
  const [verdict, setVerdict] = useState<VerdictFilter>("ALL");
  const [language, setLanguage] = useState<LanguageFilter>("ALL");
  const [page, setPage] = useState(1);

  const listPage = useMemo(
    () => fetchMySubmissionsPage({ query, verdict, language, page, pageSize: PAGE_SIZE }),
    [query, verdict, language, page],
  );

  const acceptedRate =
    stats.totalSubmissions === 0 ? null : Math.round((stats.acceptedCount / stats.totalSubmissions) * 100);
  const firstTryRate =
    stats.acceptedCount === 0 ? null : Math.round((stats.firstTryAcceptedCount / stats.acceptedCount) * 100);
  const topLanguageRate =
    stats.totalSubmissions === 0 || !stats.topLanguage
      ? null
      : Math.round((stats.topLanguageCount / stats.totalSubmissions) * 100);

  function updateFilter<T>(setter: (value: T) => void, value: T) {
    setter(value);
    setPage(1);
  }

  const columns: DataTableColumn<SubmissionListItem>[] = [
    {
      key: "submittedAt",
      header: t("table.col.submittedAt"),
      width: "160px",
      render: (row) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {new Date(row.submittedAt).toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "problem",
      header: t("table.col.problem"),
      render: (row) => (
        <Link href={`/problems/${row.problemSlug}`} className="font-semibold hover:underline">
          {row.problemCode} · {row.problemTitle}
        </Link>
      ),
    },
    {
      key: "language",
      header: t("table.col.language"),
      width: "96px",
      render: (row) => <Badge variant="neutral">{t(`language.${row.language}`)}</Badge>,
    },
    {
      key: "verdict",
      header: t("table.col.verdict"),
      width: "140px",
      render: (row) => <Badge variant={VERDICT_VARIANT[row.status] ?? "neutral"}>{t(`verdict.${row.status}`)}</Badge>,
    },
    {
      key: "testcases",
      header: t("table.col.testcases"),
      width: "88px",
      align: "right",
      render: (row) => (
        <span className="font-mono">{row.totalCount != null ? `${row.passedCount}/${row.totalCount}` : "-"}</span>
      ),
    },
    {
      key: "runtime",
      header: t("table.col.runtime"),
      width: "88px",
      align: "right",
      render: (row) => <span className="font-mono">{row.runtimeMs != null ? `${row.runtimeMs} ms` : "-"}</span>,
    },
    {
      key: "actions",
      header: "",
      width: "180px",
      align: "right",
      render: (row) => (
        <span className="flex justify-end gap-1.5">
          <Button variant="ghost" size="sm" asChild className="border border-[var(--color-border)]">
            <Link href={`/submissions/${row.id}`}>{t("table.btnResult")}</Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={row.status !== "ACCEPTED"}
            title={row.status === "ACCEPTED" ? undefined : t("table.needAccepted")}
            asChild={row.status === "ACCEPTED"}
            className="border border-[var(--color-border)]"
          >
            {row.status === "ACCEPTED" ? (
              <Link href={`/submissions/${row.id}/review`}>{t("table.btnReview")}</Link>
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

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        <StatCard label={t("stats.total")} value={stats.totalSubmissions} />
        <StatCard
          label={t("stats.accepted")}
          value={t("stats.acceptedValue", { count: stats.acceptedCount, rate: acceptedRate ?? "—" })}
        />
        <StatCard
          label={t("stats.firstTry")}
          value={t("stats.firstTryValue", {
            firstTry: stats.firstTryAcceptedCount,
            accepted: stats.acceptedCount,
            rate: firstTryRate ?? "—",
          })}
        />
        <StatCard
          label={t("stats.topLanguage")}
          value={
            stats.topLanguage
              ? t("stats.topLanguageValue", {
                  language: t(`language.${stats.topLanguage}`),
                  rate: topLanguageRate ?? "—",
                })
              : "-"
          }
        />
      </div>

      <div className="glass-card border border-[var(--color-border)] px-5 py-4">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("filter.searchLabel")}
            hideLabel
            leadingIcon={<Search className="h-3.5 w-3.5" />}
            placeholder={t("filter.searchPlaceholder")}
            value={query}
            onChange={(event) => updateFilter(setQuery, event.target.value)}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("filter.verdictLabel")}
            value={verdict}
            onValueChange={(value) => updateFilter(setVerdict, value)}
            options={[
              { value: "ALL", label: t("filter.all") },
              { value: "ACCEPTED", label: t("verdict.ACCEPTED") },
              { value: "WRONG_ANSWER", label: t("verdict.WRONG_ANSWER") },
              { value: "TIME_LIMIT_EXCEEDED", label: t("verdict.TIME_LIMIT_EXCEEDED") },
              { value: "COMPILE_ERROR", label: t("verdict.COMPILE_ERROR") },
            ]}
          />
          <SegmentedTabs
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
        </div>

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
          summary={t("footer.summary", {
            from: listPage.totalItems === 0 ? 0 : (listPage.currentPage - 1) * listPage.pageSize + 1,
            to: Math.min(listPage.currentPage * listPage.pageSize, listPage.totalItems),
            total: listPage.totalItems,
          })}
          previousLabel={t("footer.previous")}
          nextLabel={t("footer.next")}
          showPageNumbers
          pageLabel={(pageNumber) => t("footer.pageLabel", { page: pageNumber })}
        />
      </div>
    </div>
  );
}
