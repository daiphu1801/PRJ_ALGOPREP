"use client";

import { useId, useState } from "react";
import { skipToken, useQuery } from "@tanstack/react-query";
import { ChevronDown } from "lucide-react";
import {
  realReadTestcaseContent,
  TESTCASE_CATEGORIES,
  type Testcase,
  type TestcaseCategory,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Card,
  FilterMenu,
  Pagination,
  QueryError,
  Skeleton,
} from "@/shared/ui";

type CategoryFilter = "all" | "uncategorized" | TestcaseCategory;
const PAGE_SIZE = 10;

export function TestcaseCoverage({
  rows,
  approvedCount,
  problemId,
}: {
  rows: Testcase[];
  approvedCount: number;
  problemId?: string;
}) {
  const t = useT("problemInfo");
  const ta = useT("problemAuthoring");
  const detailsId = useId();
  const [category, setCategory] = useState<CategoryFilter>("all");
  const [page, setPage] = useState(1);
  const [openId, setOpenId] = useState<string | null>(null);
  const categories = TESTCASE_CATEGORIES.map((key) => {
    const cases = rows.filter((row) => row.category === key);
    return {
      key,
      label: ta(`category.${key}`),
      total: cases.length,
      approved: cases.filter((row) => row.approved).length,
    };
  });
  const options: { value: CategoryFilter; label: string }[] = [
    { value: "all", label: t("allTestcases") },
    ...categories
      .filter(({ total }) => total > 0)
      .map(({ key, label, total }) => ({
        value: key,
        label: `${label} (${total})`,
      })),
  ];
  if (rows.some((row) => !row.category)) {
    options.push({
      value: "uncategorized",
      label: t("uncategorizedTestcases"),
    });
  }
  const activeCategory = options.some((option) => option.value === category)
    ? category
    : "all";
  const filteredRows = rows
    .map((row, index) => ({ row, number: index + 1 }))
    .filter(
      ({ row }) =>
        activeCategory === "all" ||
        (row.category ?? "uncategorized") === activeCategory,
    );
  const total = filteredRows.length;
  const currentPage = Math.min(page, Math.max(1, Math.ceil(total / PAGE_SIZE)));
  const start = (currentPage - 1) * PAGE_SIZE;
  const pageRows = filteredRows.slice(start, start + PAGE_SIZE);

  return (
    <Card
      title={ta("coverageTitle")}
      description={t("coverageSummary", {
        approved: approvedCount,
        total: rows.length,
      })}
    >
      <ul className="mb-4 grid grid-cols-[repeat(auto-fit,minmax(min(100%,12rem),1fr))] gap-2">
        {categories.map(({ key, label, approved, total }) => (
          <li
            key={key}
            className="flex min-w-0 items-center justify-between gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2"
          >
            <span className="text-sm font-medium">{label}</span>
            <Badge variant={approved > 0 ? "success" : "neutral"}>
              {total > 0 ? `${approved}/${total}` : t("coverageEmpty")}
            </Badge>
          </li>
        ))}
      </ul>
      {rows.length === 0 ? (
        <p className="text-sm text-[var(--color-text-muted)]">
          {ta("emptyTestcases")}
        </p>
      ) : (
        <div className="space-y-3">
          <FilterMenu
            label={t("testcaseCategoryFilter")}
            options={options}
            value={activeCategory}
            onValueChange={(next) => {
              setCategory(next);
              setPage(1);
              setOpenId(null);
            }}
          />
          <ul className="space-y-2">
            {pageRows.map(({ row, number }) => {
              const open = openId === row.id;
              return (
                <li
                  key={row.id}
                  className="rounded-lg border border-[var(--color-border)]"
                >
                  <button
                    type="button"
                    aria-expanded={open}
                    aria-controls={open ? detailsId : undefined}
                    onClick={() => setOpenId(open ? null : row.id)}
                    className="flex w-full flex-wrap items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm hover:bg-[var(--color-surface-hover)]"
                  >
                    <span className="mr-auto font-semibold">
                      {t("testcaseNumber", { number })}
                    </span>
                    <Badge variant="neutral">
                      {ta(`visibility.${row.visibility}`)}
                    </Badge>
                    <Badge variant={row.approved ? "success" : "warn"}>
                      {row.approved ? t("testcaseApproved") : ta("statusDraft")}
                    </Badge>
                    <ChevronDown
                      aria-hidden="true"
                      size={16}
                      className={open ? "rotate-180" : undefined}
                    />
                  </button>
                  {open && (
                    <div
                      id={detailsId}
                      className="border-t border-[var(--color-border)] p-3"
                    >
                      <TestcaseContent row={row} problemId={problemId} />
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
          {total > PAGE_SIZE && (
            <Pagination
              page={currentPage}
              pageSize={PAGE_SIZE}
              total={total}
              onPageChange={(next) => {
                setPage(next);
                setOpenId(null);
              }}
              summary={t("testcasePageSummary", {
                from: start + 1,
                to: start + pageRows.length,
                total,
              })}
              previousLabel={t("previousTestcasePage")}
              nextLabel={t("nextTestcasePage")}
              showPageNumbers
              pageLabel={(number) => t("testcasePage", { number })}
              className="border-t border-[var(--color-border)]"
            />
          )}
        </div>
      )}
    </Card>
  );
}

function TestcaseContent({
  row,
  problemId,
}: {
  row: Testcase;
  problemId?: string;
}) {
  const t = useT("problemInfo");
  const ta = useT("problemAuthoring");
  const largeProblemId = row.largeSides?.length ? problemId : undefined;
  const query = useQuery({
    queryKey: ["problem-info", "testcase-content", problemId, row.id],
    queryFn: largeProblemId
      ? () => realReadTestcaseContent(largeProblemId, row.id)
      : skipToken,
    gcTime: 0,
    retry: false,
  });
  if (query.isLoading)
    return <Skeleton className="h-20 w-full" aria-busy="true" />;
  if (query.isError) return <QueryError query={query} />;
  const testcase = query.data ?? row;

  return (
    <dl className="grid gap-2 sm:grid-cols-2">
      {(
        [
          { key: "input", label: ta("exampleInput") },
          { key: "expected", label: t("expectedOutput") },
        ] as const
      ).map(({ key, label }) => (
        <div
          key={key}
          className="min-w-0 rounded-lg bg-[var(--color-track)] p-2.5"
        >
          <dt className="mb-1 text-xs font-semibold">{label}</dt>
          <dd>
            <pre className="max-h-64 overflow-auto font-mono text-sm whitespace-pre-wrap wrap-anywhere">
              {testcase.largeSides?.includes(key)
                ? ta("largeSideCell")
                : testcase[key] || t("emptyTestcaseValue")}
            </pre>
          </dd>
        </div>
      ))}
    </dl>
  );
}
