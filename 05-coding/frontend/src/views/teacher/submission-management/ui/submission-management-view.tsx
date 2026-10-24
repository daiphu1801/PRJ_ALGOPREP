// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Instructor submission list (sidebar "Bài nộp"): every submission of the learners in the classes the
// instructor is in charge of, with a class filter, a result filter and a search. Class, result, search
// text and page live in the URL (?classId=&result=&q=&page=), the one convention of every instructor
// list (INS0203 Q6). A row opens the read-only result (DEC-2026-1013) carrying the student and class.
// No stat-card strip (owner rule 2026-10-13): the list is the screen.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Eye } from "lucide-react";
import {
  initialsOfClassStudent,
  useClassStudents,
  useInstructorClasses,
} from "@/entities/class";
import {
  matchesResultFilter,
  SUBMISSION_STATUS_VARIANT,
  useInstructorReadableSubmissions,
  type SubmissionResultFilter,
} from "@/entities/submission";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Card,
  DataTable,
  FilterMenu,
  IconAction,
  PageHeader,
  Pagination,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";
import { buildSubmissionRows, type SubmissionRow } from "@/features/submission-rows";

// 20 rows per page, fixed (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric): no size picker.
const PAGE_SIZE = 20;

export function SubmissionManagementView() {
  const t = useT("instructorSubmissions");
  const tVerdict = useT("submissionResult");
  const classesQuery = useInstructorClasses();
  // Learners who left the class stay: their submissions are kept history (DEC-2026-1014-keep-learning-history-on-leave).
  const studentsQuery = useClassStudents(undefined, true);

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const classTab = searchParams.get("classId") ?? "all";
  const result = (searchParams.get("result") ??
    "all") as SubmissionResultFilter;
  const page = Math.max(1, Number(searchParams.get("page")) || 1);
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const setUrlParams = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams.toString());
    for (const [key, value] of Object.entries(patch)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    const qs = next.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };

  const readableQuery = useInstructorReadableSubmissions();
  const allRows = useMemo(
    () =>
      buildSubmissionRows(studentsQuery.data ?? [], readableQuery.data ?? []),
    [studentsQuery.data, readableQuery.data],
  );
  const rows = useMemo(() => {
    const needle = (searchParams.get("q") ?? "").trim().toLowerCase();
    return allRows.filter(
      (row) =>
        (classTab === "all" || row.classId === classTab) &&
        matchesResultFilter(row.status, result) &&
        (!needle ||
          row.studentName.toLowerCase().includes(needle) ||
          row.problemTitle.toLowerCase().includes(needle)),
    );
  }, [allRows, classTab, result, searchParams]);
  const pageRows = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const columns: DataTableColumn<SubmissionRow>[] = [
    {
      key: "student",
      header: t("col.student"),
      render: (row) => {
        const identity = (
          <>
            <span
              aria-hidden="true"
              className="glass-surface flex h-[28px] w-[28px] shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] text-[11px] font-semibold"
            >
              {initialsOfClassStudent(row.studentName)}
            </span>
            {row.studentName}
          </>
        );
        // A learner who left has no profile page in the class any more: plain name plus the note.
        return row.leftClass ? (
          <span className="flex items-center gap-2.5 font-semibold">
            {identity}
            <Badge variant="neutral">{t("leftClass")}</Badge>
          </span>
        ) : (
          <Link
            href={`/instructor/classes/${row.classId}/students/${row.studentId}?from=submissions`}
            className="flex items-center gap-2.5 font-semibold hover:underline"
          >
            {identity}
          </Link>
        );
      },
    },
    { key: "class", header: t("col.class"), render: (row) => row.className },
    {
      key: "problem",
      header: t("col.problem"),
      render: (row) => row.problemTitle,
    },
    {
      key: "language",
      header: t("col.language"),
      render: (row) => <Badge variant="neutral">{row.language}</Badge>,
    },
    {
      key: "status",
      header: t("col.status"),
      render: (row) => (
        <Badge variant={SUBMISSION_STATUS_VARIANT[row.status]}>
          {tVerdict(`verdict.${row.status}`)}
        </Badge>
      ),
    },
    {
      key: "ratio",
      header: t("col.ratio"),
      align: "right",
      render: (row) => <span className="font-mono">{row.ratio}</span>,
    },
    {
      key: "submittedAt",
      header: t("col.submittedAt"),
      render: (row) =>
        new Date(row.submittedAt).toLocaleString("vi-VN", {
          dateStyle: "short",
          timeStyle: "short",
        }),
    },
    {
      key: "action",
      header: "",
      align: "right",
      render: (row) => (
        <IconAction
          icon={Eye}
          label={t("col.btnView")}
          ariaLabel={t("col.btnViewOf", {
            student: row.studentName,
            problem: row.problemTitle,
          })}
          href={`/instructor/submissions/${row.submissionId}?studentId=${row.studentId}&classId=${row.classId}&from=submissions`}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("header.title")}
        description={t("header.subtitle")}
      />

      <Card>
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("search")}
            hideLabel
            placeholder={t("search")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setUrlParams({ q: event.target.value.trim(), page: null });
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <FilterMenu
            label={t("classFilter")}
            value={classTab}
            onValueChange={(value) =>
              setUrlParams({
                classId: value === "all" ? null : value,
                page: null,
              })
            }
            options={[
              { value: "all", label: t("filterAll") },
              ...(classesQuery.data ?? []).map((c) => ({
                value: c.id,
                label: c.name,
              })),
            ]}
          />
          <FilterMenu
            label={t("resultFilter")}
            value={result}
            onValueChange={(value) =>
              setUrlParams({
                result: value === "all" ? null : value,
                page: null,
              })
            }
            options={[
              { value: "all", label: t("filterAll") },
              { value: "accepted", label: t("result.accepted") },
              { value: "failed", label: t("result.failed") },
            ]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("resultCount", { shown: rows.length, total: allRows.length })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={pageRows}
          rowKey={(row) => row.key}
          status={
            studentsQuery.isLoading
              ? "loading"
              : studentsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={
            (classesQuery.data ?? []).length === 0
              ? t("emptyNoClass")
              : allRows.length === 0
                ? t("emptyNoSubmission")
                : t("empty")
          }
          minWidth={960}
        />

        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={rows.length}
          onPageChange={(value) =>
            setUrlParams({ page: value > 1 ? String(value) : null })
          }
          summary={t("pageSummary", {
            page,
            total: Math.max(1, Math.ceil(rows.length / PAGE_SIZE)),
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>
    </div>
  );
}
