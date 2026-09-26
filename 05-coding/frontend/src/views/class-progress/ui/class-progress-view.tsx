// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout theo 09-layoutBase/Giáo viên - Tiến độ học viên.dc.html: tiêu đề + phụ đề (:117-118), khối
// trên 2 cột — biểu đồ đường "Điểm trung bình theo lớp" (:122-134) và "Cần chú ý" (:136-150), thanh lọc
// gồm ô tìm học viên + tab lớp + đếm kết quả (:154-164), bảng danh sách học viên với cột Xu hướng dạng
// sparkline (:168-169 header, :182 render) (:167-187). Sidebar mục "Tiến độ học viên" active (:244-250).
// 02-bd/screens/teacher/INS0203_class_progress.md Sheet 4.4: bố cục 2 cột tương ứng.
//
// Score trend simplified to ONE aggregate line (avg of the filtered classes) instead of one polyline
// per class — LineChartWithTotal (shared/ui) only plots a single series; a real multi-line chart
// primitive is a bigger addition than one screen justifies, [SoT: Suy luận]. The mockup itself draws
// exactly two lines regardless of how many classes exist (line1/line2 are hardcoded arrays, :126-128,
// :278-279, not derived from the class count) — so "one line" here is a real simplification against a
// real two-line mockup, not a match dressed up as one.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  initialsOfClassStudent,
  useClassAttention,
  useClassScoreTrend,
  useClassStudents,
  useInstructorClasses,
  type StudentStatus,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Card,
  DataTable,
  LineChartWithTotal,
  PageHeader,
  Pagination,
  SegmentedTabs,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const STATUS_VARIANT: Record<StudentStatus, BadgeVariant> = {
  insufficient_data: "neutral",
  absent: "negative",
  needs_support: "warn",
  watch: "blue",
  on_track: "success",
};

const PAGE_SIZE = 20;

// Bare-svg sparkline for the "Xu hướng" column (dc.html:182) — point math mirrors
// shared/ui/charts/stat-card-with-sparkline.tsx, kept local since this is the only table-cell use.
function Sparkline({ points }: { points: number[] }) {
  if (points.length < 2) return <span className="text-[var(--color-text-muted)]">-</span>;
  const max = Math.max(...points);
  const min = Math.min(...points);
  const range = max - min || 1;
  const path = points
    .map((v, i) => `${(i / (points.length - 1)) * 100},${24 - ((v - min) / range) * 24}`)
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

  const [classTab, setClassTab] = useState("all");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);

  const filterClassId = classTab === "all" ? undefined : classTab;
  const trendQuery = useClassScoreTrend(filterClassId);
  const attentionQuery = useClassAttention(filterClassId);
  const studentsQuery = useClassStudents(filterClassId);

  const filteredStudents = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return (studentsQuery.data ?? []).filter((s) => !needle || s.name.toLowerCase().includes(needle));
  }, [studentsQuery.data, query]);

  const pageRows = filteredStudents.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const trendPoints = useMemo(() => {
    const series = trendQuery.data?.series ?? [];
    const labels = trendQuery.data?.weekLabels ?? [];
    return labels.map((label, index) => ({
      label,
      value: series.length
        ? Math.round((series.reduce((sum, s) => sum + (s.points[index] ?? 0), 0) / series.length) * 10) / 10
        : 0,
    }));
  }, [trendQuery.data]);
  const lastPoint = trendPoints.at(-1)?.value;

  const columns: DataTableColumn<NonNullable<typeof studentsQuery.data>[number]>[] = [
    {
      key: "name",
      header: t("studentList.col.studentName"),
      render: (row) => (
        <Link href={`/instructor/classes/${row.classId}/students/${row.id}`} className="flex items-center gap-2.5 font-semibold hover:underline">
          <span aria-hidden="true" className="glass-surface flex h-[28px] w-[28px] shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] text-[11px] font-semibold">
            {initialsOfClassStudent(row.name)}
          </span>
          {row.name}
        </Link>
      ),
    },
    { key: "className", header: t("studentList.col.className"), render: (row) => row.className },
    { key: "avgScore", header: t("studentList.col.avgScore"), align: "right", render: (row) => (row.avgScore === null ? "-" : row.avgScore.toFixed(1)) },
    { key: "completion", header: t("studentList.col.completion"), align: "right", render: (row) => `${row.completionPct}%` },
    { key: "streak", header: t("studentList.col.streak"), align: "right", render: (row) => (row.streakDays > 0 ? `${row.streakDays} ngày` : "-") },
    // dc.html:169 header "Xu hướng", :182 renders a per-student sparkline path from trend points.
    { key: "trend", header: t("studentList.col.trend"), render: (row) => <Sparkline points={row.trend} /> },
    { key: "lastActive", header: t("studentList.col.lastActive"), render: (row) => row.lastActiveLabel },
  ];

  return (
    <div>
      <PageHeader title={t("header.title")} description={t("header.subtitle")} />

      <div className="mb-4 grid grid-cols-1 items-start gap-3.5 lg:grid-cols-[1.3fr_1fr]">
        <Card title={t("scoreTrend.title")} description={t("scoreTrend.rangeLabel")}>
          {trendPoints.length ? (
            <LineChartWithTotal points={trendPoints} totalLabel={t("scoreTrend.rangeLabel")} totalValue={lastPoint !== undefined ? lastPoint.toFixed(1) : "-"} />
          ) : (
            <p className="text-[13px] text-[var(--color-text-muted)]">{t("empty")}</p>
          )}
        </Card>

        <Card title={t("attention.title")}>
          <ul className="flex flex-col gap-2.5">
            {(attentionQuery.data ?? []).length === 0 ? (
              <li className="text-[13px] text-[var(--color-text-muted)]">{t("empty")}</li>
            ) : (
              attentionQuery.data!.map((item) => (
                <li key={item.studentId} className="flex items-center justify-between gap-3 rounded-lg border border-[var(--color-border)] px-3 py-2">
                  <div className="min-w-0">
                    <Link href={`/instructor/classes/${item.classId}/students/${item.studentId}`} className="block truncate font-semibold hover:underline">
                      {item.name}
                    </Link>
                    <p className="truncate text-[12px] text-[var(--color-text-muted)]">{item.reason}</p>
                  </div>
                  <Badge variant={STATUS_VARIANT[item.status]}>{t(`status.${item.status}`)}</Badge>
                </li>
              ))
            )}
          </ul>
        </Card>
      </div>

      <Card title={t("studentList.title")}>
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("studentList.searchBox")}
            hideLabel
            placeholder={t("studentList.searchBox")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(1);
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("studentList.classTabs")}
            value={classTab}
            onValueChange={(value) => {
              setClassTab(value);
              setPage(1);
            }}
            options={[{ value: "all", label: t("filterAll") }, ...classes.map((c) => ({ value: c.id, label: c.name }))]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("studentList.resultCount", { shown: filteredStudents.length, total: studentsQuery.data?.length ?? 0 })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={pageRows}
          rowKey={(row) => row.id}
          status={studentsQuery.isLoading ? "loading" : studentsQuery.isError ? "error" : "ready"}
          emptyMessage={t("empty")}
          minWidth={860}
        />

        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={filteredStudents.length}
          onPageChange={setPage}
          summary={t("pageSummary", { page, total: Math.max(1, Math.ceil(filteredStudents.length / PAGE_SIZE)) })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>
    </div>
  );
}
