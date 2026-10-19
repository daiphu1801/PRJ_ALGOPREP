// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// THE Instructor student list — restructured 2026-09-27 (owner: "1 pages quản lý lớp học, 1 pages
// quản lý danh sách học viên"). Before this, the same students were listed TWICE: here with
// progress columns, and again on class_management with status + "Gỡ khỏi lớp". The two tables are
// merged into this one; class_management now only manages classes.
//
// Kept from 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html: tiêu đề + phụ đề (:117-118), thanh
// lọc gồm ô tìm học viên + tab lớp + đếm kết quả (:154-164), bảng với cột Xu hướng dạng sparkline
// (:168-169 header, :182 render) (:167-187).
//
// Dropped from the mockup, both on the owner's call:
//   - the "Điểm trung bình theo lớp" chart (:122-134) — instructor_overview plots the same series;
//   - the "Cần chú ý" list (:136-150) — it restated what the "Trạng thái" column of this very table
//     already says, one block above it ("tránh UX cuộn quá nhiều"). The column is the single place
//     a student's standing is shown now.
//
// Reached from the sidebar, and from a class card's "Xem học viên" with ?classId= preselecting that
// class's tab.
"use client";

import { UserMinus } from "lucide-react";
import { useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  initialsOfClassStudent,
  useClassStudents,
  useInstructorClasses,
  useRemoveStudent,
  STUDENT_STATUS_VARIANT,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Card,
  ConfirmDialog,
  DataTable,
  IconAction,
  PageHeader,
  Pagination,
  FilterMenu,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

// 20 rows per page, fixed (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric): no size picker.
const PAGE_SIZE = 20;

// Bare-svg sparkline for the "Xu hướng" column (dc.html:182) — point math mirrors
// shared/ui/charts/stat-card-with-sparkline.tsx, kept local since this is the only table-cell use.
function Sparkline({ points }: { points: number[] }) {
  if (points.length < 2)
    return <span className="text-[var(--color-text-muted)]">-</span>;
  const max = Math.max(...points);
  const min = Math.min(...points);
  const range = max - min || 1;
  const path = points
    .map(
      (v, i) =>
        `${(i / (points.length - 1)) * 100},${24 - ((v - min) / range) * 24}`,
    )
    .join(" ");
  const up = points.at(-1)! >= points[0]!;
  return (
    <svg viewBox="0 0 100 24" className="h-6 w-[90px]" aria-hidden="true">
      <polyline
        points={path}
        fill="none"
        stroke={up ? "var(--color-success)" : "var(--color-danger)"}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function ClassProgressView() {
  const t = useT("classProgress");
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  // Class tab, search text and page live in the URL (?classId=&q=&page=, INS0203 Q6, owner
  // 2026-10-13) so Back from a student's detail page restores them. "Xem học viên" on a class card
  // lands here already filtered the same way.
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const classTab = searchParams.get("classId") ?? "all";
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
  const setPage = (value: number) =>
    setUrlParams({ page: value > 1 ? String(value) : null });
  const [removeTarget, setRemoveTarget] = useState<{
    id: string;
    name: string;
  } | null>(null);
  const removeStudent = useRemoveStudent();

  const filterClassId = classTab === "all" ? undefined : classTab;
  const studentsQuery = useClassStudents(filterClassId);

  const filteredStudents = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return (studentsQuery.data ?? []).filter(
      (s) => !needle || s.name.toLowerCase().includes(needle),
    );
  }, [studentsQuery.data, query]);

  const pageRows = filteredStudents.slice(
    (page - 1) * PAGE_SIZE,
    page * PAGE_SIZE,
  );

  const columns: DataTableColumn<
    NonNullable<typeof studentsQuery.data>[number]
  >[] = [
    {
      key: "name",
      header: t("studentList.col.studentName"),
      render: (row) => (
        <Link
          href={`/instructor/classes/${row.classId}/students/${row.id}?from=students`}
          className="flex items-center gap-2.5 font-semibold hover:underline"
        >
          <span
            aria-hidden="true"
            className="glass-surface flex h-[28px] w-[28px] shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] text-[11px] font-semibold"
          >
            {initialsOfClassStudent(row.name)}
          </span>
          {row.name}
        </Link>
      ),
    },
    {
      key: "className",
      header: t("studentList.col.className"),
      render: (row) => row.className,
    },
    {
      key: "avgScore",
      header: t("studentList.col.avgScore"),
      align: "right",
      render: (row) => (row.avgScore === null ? "-" : row.avgScore.toFixed(1)),
    },
    {
      key: "completion",
      header: t("studentList.col.completion"),
      align: "right",
      render: (row) => `${row.completionPct}%`,
    },
    {
      key: "streak",
      header: t("studentList.col.streak"),
      align: "right",
      render: (row) => (row.streakDays > 0 ? `${row.streakDays} ngày` : "-"),
    },
    // dc.html:169 header "Xu hướng", :182 renders a per-student sparkline path from trend points.
    {
      key: "trend",
      header: t("studentList.col.trend"),
      render: (row) => <Sparkline points={row.trend} />,
    },
    // Carried over from class_management's student table when the two merged: the status badge is
    // now the ONLY place a student's standing shows, so it cannot be dropped with the "Cần chú ý"
    // block it used to duplicate.
    {
      key: "status",
      header: t("studentList.col.status"),
      render: (row) => (
        <Badge variant={STUDENT_STATUS_VARIANT[row.status]}>
          {t(`status.${row.status}`)}
        </Badge>
      ),
    },
    {
      key: "lastActive",
      header: t("studentList.col.lastActive"),
      render: (row) => row.lastActiveLabel,
    },
    {
      key: "remove",
      header: "",
      align: "right",
      render: (row) => (
        <IconAction
          icon={UserMinus}
          label={t("btnRemoveStudent")}
          ariaLabel={`${t("btnRemoveStudent")} ${row.name}`}
          tone="danger"
          onClick={() => setRemoveTarget({ id: row.id, name: row.name })}
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
            label={t("studentList.searchBox")}
            hideLabel
            placeholder={t("studentList.searchBox")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setUrlParams({ q: event.target.value.trim(), page: null });
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <FilterMenu
            label={t("studentList.classTabs")}
            value={classTab}
            onValueChange={(value) =>
              setUrlParams({
                classId: value === "all" ? null : value,
                page: null,
              })
            }
            options={[
              { value: "all", label: t("filterAll") },
              ...classes.map((c) => ({ value: c.id, label: c.name })),
            ]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("studentList.resultCount", {
              shown: filteredStudents.length,
              total: studentsQuery.data?.length ?? 0,
            })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={pageRows}
          rowKey={(row) => row.id}
          status={
            studentsQuery.isLoading
              ? "loading"
              : studentsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("empty")}
          minWidth={860}
        />

        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={filteredStudents.length}
          onPageChange={setPage}
          summary={t("pageSummary", {
            page,
            total: Math.max(1, Math.ceil(filteredStudents.length / PAGE_SIZE)),
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget) {
            const name = removeTarget.name;
            removeStudent.mutate(removeTarget.id, {
              onSuccess: () => toast.success(t("toast.removed", { name })),
              onError: () => toast.error(t("toast.failed")),
            });
          }
          setRemoveTarget(null);
        }}
        title={t("popup.removeStudentTitle", {
          name: removeTarget?.name ?? "",
        })}
        confirmLabel={t("popup.removeStudentConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("popup.removeStudentBody")}
      </ConfirmDialog>
    </div>
  );
}
