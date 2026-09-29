// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html: header + subtitle + "+ Gán từ ngân hàng bài
// toán" button (:115-120), 4 stat cards — bài đã gán, lượt nộp tuần này, AC trung bình, cần chấm
// tay (:123-134, values :259-264), filter row: search + class tabs + result count (:138-149,
// :266-271), assigned-problem table incl. the "Gán cho lớp" column (:151-170, :152-153, :161).
// "+ Gán từ ngân hàng bài toán" links to problem_list (not built by this screen —
// DEC-2026-0831-class-assignments-round2); "Chi tiết" wires to problem_detail per RD Q8 (closed),
// not the interview-bank link the prototype file is described as having in
// 01-rd/screens/teacher/INS0202_class_assignments.md:80-84. The remove popup asks for a class
// first only when the row spans more than one class, matching the N-N problem-class relation
// carried by "Gán cho lớp" (:161) — RD Q9 (closed).
//
// Nav sibling, not a tab of one page: this screen is the "Bài tập của tôi" (BT) sidebar item,
// independent from "Lớp của tôi" (LH) [09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useAssignmentSummary, useClassAssignments, useRemoveAssignment, type AssignedProblem } from "@/entities/class-assignment";
import { useInstructorClasses } from "@/entities/class";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  PageHeader,
  SegmentedTabs,
  SelectField,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const DIFFICULTY_VARIANT: Record<AssignedProblem["difficulty"], BadgeVariant> = {
  EASY: "success",
  MEDIUM: "warn",
  HARD: "negative",
};

export function ClassAssignmentsView() {
  const t = useT("classAssignments");
  const summaryQuery = useAssignmentSummary();
  const assignmentsQuery = useClassAssignments();
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  const [query, setQuery] = useState("");
  const [classTab, setClassTab] = useState("all");
  const [removeTarget, setRemoveTarget] = useState<AssignedProblem | null>(null);
  const [removeClassChoice, setRemoveClassChoice] = useState<string>("");
  const removeAssignment = useRemoveAssignment();

  const rows = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return (assignmentsQuery.data ?? []).filter(
      (row) =>
        (classTab === "all" || row.assignedClassIds.includes(classTab)) &&
        (!needle || row.title.toLowerCase().includes(needle)),
    );
  }, [assignmentsQuery.data, query, classTab]);

  const columns: DataTableColumn<AssignedProblem>[] = [
    { key: "title", header: t("assignmentList.col.title"), render: (row) => row.title },
    { key: "topic", header: t("assignmentList.col.topic"), render: (row) => row.topic },
    {
      key: "difficulty",
      header: t("assignmentList.col.difficulty"),
      render: (row) => <Badge variant={DIFFICULTY_VARIANT[row.difficulty]}>{t(`difficulty.${row.difficulty}`)}</Badge>,
    },
    {
      key: "assignedClasses",
      header: t("assignmentList.col.assignedClasses"),
      render: (row) => <span className="truncate">{row.assignedClassNames.join(", ")}</span>,
    },
    {
      key: "submissionCount",
      header: t("assignmentList.col.submissionCount"),
      align: "right",
      render: (row) => <span className="font-mono">{row.submissionCount.toLocaleString("vi-VN")}</span>,
    },
    { key: "acRate", header: t("assignmentList.col.acRate"), align: "right", render: (row) => `${row.acRate}%` },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" asChild>
            <Link href={`/problems/${row.id}`}>{t("assignmentList.col.btnDetail")}</Link>
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-[var(--color-admin-negative)]"
            onClick={() => {
              setRemoveTarget(row);
              setRemoveClassChoice(classTab !== "all" ? classTab : row.assignedClassIds[0] ?? "");
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
        description={t("header.summary", { count: summary?.assignedCount ?? 0 })}
        actions={
          <Button variant="cta" size="sm" asChild>
            <Link href="/problems">{t("header.btnAssignFromBank")}</Link>
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
            onChange={(event) => setQuery(event.target.value)}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("assignmentList.classTabs")}
            value={classTab}
            onValueChange={setClassTab}
            options={[{ value: "all", label: t("filterAll") }, ...classes.map((c) => ({ value: c.id, label: c.name }))]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("assignmentList.resultCount", { shown: rows.length, total: assignmentsQuery.data?.length ?? 0 })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={rows}
          rowKey={(row) => row.id}
          status={assignmentsQuery.isLoading ? "loading" : assignmentsQuery.isError ? "error" : "ready"}
          emptyMessage={t("emptyAssignments")}
          minWidth={900}
        />
      </Card>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget && removeClassChoice) {
            removeAssignment.mutate({ problemId: removeTarget.id, classId: removeClassChoice });
          }
          setRemoveTarget(null);
        }}
        title={t("popup.removeAssignmentTitle", { title: removeTarget?.title ?? "" })}
        confirmLabel={t("popup.removeAssignmentConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        <p className="mb-3">{t("popup.removeAssignmentBody")}</p>
        {removeTarget && removeTarget.assignedClassIds.length > 1 ? (
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
