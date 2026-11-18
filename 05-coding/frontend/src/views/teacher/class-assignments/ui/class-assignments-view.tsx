// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html: header + subtitle + "+ Gán từ ngân hàng bài
// toán" button (:115-120), 4 stat cards — bài đã gán, lượt nộp tuần này, AC trung bình, cần chấm
// tay (:123-134, values :259-264), filter row: search + class tabs + result count (:138-149,
// :266-271), assigned-problem table incl. the "Gán cho lớp" column (:151-170, :152-153, :161).
// "+ Gán từ ngân hàng bài toán" opens the "Giao bài cho lớp" popup on this screen (2026-10-11;
// DEC-2026-0831-class-assignments-round2 put it on problem_list, which is a learner screen); "Chi tiết" wires to problem_detail per RD Q8 (closed),
// not the interview-bank link the prototype file is described as having in
// 01-rd/screens/teacher/INS0202_class_assignments.md:80-84. The remove popup asks for a class
// first only when the row spans more than one class, matching the N-N problem-class relation
// carried by "Gán cho lớp" (:161) — RD Q9 (closed).
//
// Nav sibling, not a tab of one page: this screen is the "Bài giao cho lớp" (BT) sidebar item,
// independent from "Lớp của tôi" (LH) [09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  useAssignmentSummary,
  useClassAssignmentsPage,
  useRemoveAssignment,
  type AssignedRow,
} from "@/entities/class-assignment";
import { useInstructorClasses } from "@/entities/class";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { AssignProblemDialog } from "./assign-problem-dialog";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  Pagination,
  FilterMenu,
  SelectField,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

// 20 rows per page for a table that merges many rows (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric).
const PAGE_SIZE = 20;

/** First topic alphabetically plus "+n"; the full list stays in the tooltip (INS0202 Q5). */
function topicSummary(row: AssignedRow): { text: string; title: string } {
  const all = [...(row.topics?.length ? row.topics : [row.topic])].sort(
    (a, b) => a.localeCompare(b, "vi"),
  );
  return {
    text: all.length > 1 ? `${all[0]} +${all.length - 1}` : (all[0] ?? ""),
    title: all.join(", "),
  };
}

export function ClassAssignmentsView() {
  const t = useT("classAssignments");
  const levelList = useProblemLevels();
  const summaryQuery = useAssignmentSummary();
  const apiMessage = useApiErrorMessage();
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  // Class, search text and page live in the URL (?classId=&q=&page=), the one convention of every list.
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
  // The server filters by class and title and pages by 20; typing waits for a pause before it asks.
  const q = useDebounced((searchParams.get("q") ?? "").trim());
  const assignmentsQuery = useClassAssignmentsPage({
    classId: classTab,
    q,
    page,
  });
  const [assigning, setAssigning] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<AssignedRow | null>(null);
  const [removeClassChoice, setRemoveClassChoice] = useState<string>("");
  const removeAssignment = useRemoveAssignment();

  const pageRows = assignmentsQuery.data?.items ?? [];
  const totalRows = assignmentsQuery.data?.page.totalItems ?? 0;

  // Figures inside the selected class; "Tất cả" shows the problem's totals (INS0202 Q11).
  const statsFor = (row: AssignedRow) =>
    (classTab !== "all" ? row.classStats?.[classTab] : undefined) ?? row;

  const columns: DataTableColumn<AssignedRow>[] = [
    {
      key: "title",
      header: t("assignmentList.col.title"),
      render: (row) => (
        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span className="whitespace-nowrap">{row.title}</span>
          {row.withdrawn ? (
            <Badge variant="warn">{t("assignmentList.col.withdrawn")}</Badge>
          ) : null}
        </span>
      ),
    },
    {
      key: "topic",
      header: t("assignmentList.col.topic"),
      render: (row) => {
        const topic = topicSummary(row);
        return <span title={topic.title}>{topic.text}</span>;
      },
    },
    {
      key: "difficulty",
      header: t("assignmentList.col.difficulty"),
      render: (row) => (
        <Badge variant={problemLevelTone(levelList, row.difficulty)}>
          {problemLevelLabel(levelList, row.difficulty)}
        </Badge>
      ),
    },
    {
      key: "assignedClasses",
      header: t("assignmentList.col.assignedClasses"),
      render: (row) => (
        <span className="truncate">{row.assignedClassNames.join(", ")}</span>
      ),
    },
    {
      key: "submissionCount",
      header: t("assignmentList.col.submissionCount"),
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {statsFor(row).submissionCount === null
            ? "-"
            : statsFor(row).submissionCount!.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "acRate",
      header: t("assignmentList.col.acRate"),
      align: "right",
      render: (row) =>
        statsFor(row).acRate === null ? "-" : `${statsFor(row).acRate}%`,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" asChild>
            <Link href={`/instructor/problems/${row.id}`}>
              {t("assignmentList.col.btnDetail")}
            </Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-[var(--color-admin-negative)]"
            onClick={() => {
              setRemoveTarget(row);
              setRemoveClassChoice(
                classTab !== "all" ? classTab : (row.assignedClassIds[0] ?? ""),
              );
            }}
          >
            {t("assignmentList.col.btnRemove")}
          </Button>
        </div>
      ),
    },
  ];

  const summary = summaryQuery.data;

  return (
    <div>
      <PageHeader
        title={t("header.title")}
        // The count comes from #50; while it is unknown no number is shown (never a made-up 0).
        // The class average sits on the same line (INS0202 V0.14: no stat strip on this screen); "-" when
        // #50 sends null (no submissions yet, or the statistics are down), never a made-up 0%.
        description={
          summary
            ? `${t("header.summary", { count: summary.assignedCount })} · ${t(
                "header.avgAcRate",
                {
                  value:
                    summary.avgAcRate == null ? "-" : `${summary.avgAcRate}%`,
                },
              )}`
            : undefined
        }
        actions={
          <Button variant="cta" size="sm" onClick={() => setAssigning(true)}>
            {t("header.btnAssignFromBank")}
          </Button>
        }
      />

      {/* Stat strip dropped 2026-09-27 (owner): every figure on it — số lớp, tổng học viên, cần
          chấm tay, điểm TB — is already the stat strip of instructor_overview, and repeating it on
          each screen pushed the actual worklist below the fold. These screens now open straight on
          their list + filter bar. Deliberate divergence from the mockup's own per-screen strip. */}
      <Card>
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("assignmentList.search")}
            hideLabel
            placeholder={t("assignmentList.search")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setUrlParams({ q: event.target.value.trim(), page: null });
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <FilterMenu
            label={t("assignmentList.classTabs")}
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
            {t("assignmentList.resultCount", {
              shown: totalRows,
              total: summary?.assignedCount ?? "-",
            })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={pageRows}
          rowKey={(row) => row.id}
          status={
            assignmentsQuery.isLoading
              ? "loading"
              : assignmentsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("emptyAssignments")}
          errorMessage={t("loadFailed")}
          minWidth={900}
        />

        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={totalRows}
          onPageChange={(value) =>
            setUrlParams({ page: value > 1 ? String(value) : null })
          }
          summary={t("pageSummary", {
            page,
            total: Math.max(1, assignmentsQuery.data?.page.totalPages ?? 1),
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <AssignProblemDialog
        open={assigning}
        onClose={() => setAssigning(false)}
        defaultClassId={classTab === "all" ? undefined : classTab}
      />

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget && removeClassChoice) {
            const title = removeTarget.title;
            removeAssignment.mutate(
              { problemId: removeTarget.id, classId: removeClassChoice },
              {
                onSuccess: () => toast.success(t("toast.removed", { title })),
                onError: (error) => toast.error(apiMessage(error)),
              },
            );
          }
          setRemoveTarget(null);
        }}
        title={t("popup.removeAssignmentTitle", {
          title: removeTarget?.title ?? "",
        })}
        confirmLabel={t("popup.removeAssignmentConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        <p className="mb-3">{t("popup.removeAssignmentBody")}</p>
        {removeTarget &&
        classTab === "all" &&
        removeTarget.assignedClassIds.length > 1 ? (
          <SelectField
            label={t("popup.removeClassChoice")}
            value={removeClassChoice}
            onChange={(event) => setRemoveClassChoice(event.target.value)}
            options={removeTarget.assignedClassIds.map((id, index) => ({
              value: id,
              label: removeTarget.assignedClassNames[index] ?? id,
            }))}
          />
        ) : null}
      </ConfirmDialog>
    </div>
  );
}
