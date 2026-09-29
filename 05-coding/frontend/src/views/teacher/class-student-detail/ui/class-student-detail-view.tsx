// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 02-bd/screens/teacher/INS0204_class_student_detail.md — the ONE screen in the instructor cluster
// with no 09-layoutBase prototype at all (BD Sheet 0 header). Layout below is [SoT: Suy luận],
// following the BD's own three blocks (profile header + stat cards, assignment progress table,
// submission history table) and the sibling screens' visual language rather than inventing a
// fourth. Assigned-problem list comes from entities/class-assignment (ListClassAssignments, same
// endpoint class_assignments already names) filtered to this class; "topic breakdown" is derived
// from that same list client-side per BD 4.1 point 1 ("dẫn xuất từ nhóm trước, không gọi riêng").
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useClassAssignments } from "@/entities/class-assignment";
import { initialsOfClassStudent, useClassStudentDetail, useInstructorClasses, useRemoveStudent } from "@/entities/class";
import { useT } from "@/shared/i18n";
import { Badge, Button, Card, ConfirmDialog, DataTable, PageHeader, StatCard, type DataTableColumn } from "@/shared/ui";

// Deterministic mock submission history — no dedicated entity yet (this screen has no
// 03-dd/api/judge-orchestration.md contract to type a mock against), so it is generated in-view
// from the assigned-problem list rather than invented in a separate untyped file.
function buildSubmissionHistory(problemTitles: string[]) {
  return problemTitles.slice(0, 6).map((title, index) => ({
    id: `sub-${index}`,
    problemTitle: title,
    language: index % 2 === 0 ? "Java" : "Python",
    status: index % 3 === 0 ? "ACCEPTED" : index % 3 === 1 ? "WRONG_ANSWER" : "TIME_LIMIT",
    testcaseRatio: index % 3 === 0 ? "10/10" : index % 3 === 1 ? "4/10" : "7/10",
    submittedAtLabel: `${index + 1} ngày trước`,
  }));
}

export function ClassStudentDetailView({ classId, studentId }: { classId: string; studentId: string }) {
  const t = useT("classStudentDetail");
  const classesQuery = useInstructorClasses();
  const studentQuery = useClassStudentDetail(studentId, classId);
  const assignmentsQuery = useClassAssignments();
  const removeStudent = useRemoveStudent();
  const [confirmingRemove, setConfirmingRemove] = useState(false);

  const className = classesQuery.data?.find((c) => c.id === classId)?.name ?? classId;

  const classAssignments = useMemo(
    () => (assignmentsQuery.data ?? []).filter((row) => row.assignedClassIds.includes(classId)),
    [assignmentsQuery.data, classId],
  );

  const topicBreakdown = useMemo(() => {
    const byTopic = new Map<string, number>();
    for (const row of classAssignments) byTopic.set(row.topic, (byTopic.get(row.topic) ?? 0) + 1);
    return [...byTopic.entries()];
  }, [classAssignments]);

  const submissionHistory = useMemo(
    () => buildSubmissionHistory(classAssignments.map((row) => row.title)),
    [classAssignments],
  );

  const student = studentQuery.data;

  const assignmentColumns: DataTableColumn<(typeof classAssignments)[number]>[] = [
    { key: "title", header: t("assignmentProgress.col.title"), render: (row) => row.title },
    { key: "topic", header: t("assignmentProgress.col.topic"), render: (row) => row.topic },
    { key: "acRate", header: t("assignmentProgress.col.acRate"), align: "right", render: (row) => `${row.acRate}%` },
  ];

  const historyColumns: DataTableColumn<(typeof submissionHistory)[number]>[] = [
    { key: "problem", header: t("submissionHistory.col.problem"), render: (row) => row.problemTitle },
    { key: "language", header: t("submissionHistory.col.language"), render: (row) => row.language },
    { key: "status", header: t("submissionHistory.col.status"), render: (row) => <Badge variant={row.status === "ACCEPTED" ? "success" : "negative"}>{row.status}</Badge> },
    { key: "ratio", header: t("submissionHistory.col.ratio"), align: "right", render: (row) => row.testcaseRatio },
    { key: "submittedAt", header: t("submissionHistory.col.submittedAt"), render: (row) => row.submittedAtLabel },
    {
      key: "action",
      header: "",
      align: "right",
      render: (row) => (
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/submissions/${row.id}`}>{t("submissionHistory.btnViewResult")}</Link>
        </Button>
      ),
    },
  ];

  if (studentQuery.isLoading) {
    return <p className="p-6 text-sm text-[var(--color-text-muted)]">{t("loading")}</p>;
  }

  if (!student) {
    return (
      <div className="p-6">
        <p className="text-sm text-[var(--color-text-muted)]">{t("notFound")}</p>
        <Link href={`/instructor/students?classId=${classId}`} className="mt-2 inline-block text-[var(--color-primary)] underline">
          {t("backLink")}
        </Link>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title={student.name}
        description={
          <Link href={`/instructor/classes`} className="hover:underline">
            {className}
          </Link>
        }
        actions={
          <Button variant="ghost" size="sm" className="text-[var(--color-admin-negative)]" onClick={() => setConfirmingRemove(true)}>
            {t("btnRemoveFromClass")}
          </Button>
        }
      />

      <div className="mb-4 flex items-center gap-3">
        <span aria-hidden="true" className="glass-surface flex h-12 w-12 items-center justify-center rounded-full border border-[var(--color-border)] text-base font-semibold">
          {initialsOfClassStudent(student.name)}
        </span>
        <div>
          <Link href={`/instructor/students?classId=${classId}`} className="text-[13px] font-semibold text-[var(--color-text-muted)] hover:underline">
            {t("backLink")}
          </Link>
        </div>
      </div>

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        <StatCard label={t("stats.avgScore")} value={student.avgScore === null ? "-" : student.avgScore.toFixed(1)} />
        <StatCard label={t("stats.completion")} value={`${student.completionPct}%`} />
        <StatCard label={t("stats.streak")} value={student.streakDays > 0 ? `${student.streakDays} ngày` : "-"} />
        <StatCard label={t("stats.lastActive")} value={student.lastActiveLabel} />
      </div>

      <Button variant="ghost" size="sm" asChild className="mb-4 border border-[var(--color-border)]">
        <Link href={`/instructor/grading?studentId=${studentId}&classId=${classId}`}>{t("linkViewGrading")}</Link>
      </Button>

      <Card title={t("topicProgress.title")} className="mb-4">
        <ul className="flex flex-wrap gap-2">
          {topicBreakdown.map(([topic, count]) => (
            <li key={topic}>
              <Badge variant="blue">{t("topicProgress.item", { topic, count })}</Badge>
            </li>
          ))}
        </ul>
      </Card>

      <Card title={t("assignmentProgress.title")} className="mb-4">
        <DataTable
          caption={t("assignmentProgress.title")}
          columns={assignmentColumns}
          rows={classAssignments}
          rowKey={(row) => row.id}
          emptyMessage={t("empty")}
          minWidth={600}
        />
      </Card>

      <Card title={t("submissionHistory.title")}>
        <DataTable
          caption={t("submissionHistory.title")}
          columns={historyColumns}
          rows={submissionHistory}
          rowKey={(row) => row.id}
          emptyMessage={t("empty")}
          minWidth={760}
        />
      </Card>

      <ConfirmDialog
        open={confirmingRemove}
        onClose={() => setConfirmingRemove(false)}
        onConfirm={() => {
          removeStudent.mutate(studentId);
          setConfirmingRemove(false);
        }}
        title={t("popup.removeTitle", { name: student.name })}
        confirmLabel={t("popup.removeConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("popup.removeBody")}
      </ConfirmDialog>
    </div>
  );
}
