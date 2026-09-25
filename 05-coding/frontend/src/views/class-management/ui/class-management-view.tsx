// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout theo 09-layoutBase/Giáo viên - Lớp của tôi.dc.html: header + nút "+ Tạo lớp mới"
// (:115-121), 4 thẻ thống kê (:123-134, dữ liệu :253-258), lưới thẻ lớp (:136-157, dữ liệu
// :260-265), bảng "Danh sách học viên" với tab lọc theo lớp, cột Trạng thái + Hoạt động
// (:159-188, dữ liệu :267-284).
//
// Layout follows 02-bd/screens/teacher/INS0201_class_management.md Sheet 4.4 for the rest of the
// structure (header row, 4 stat cards, class card grid, student table with class tabs).
// The action menu / class-card click filter / student "Gỡ khỏi lớp" / student name link (Sheet 4.4
// last paragraph) are all BD-added — prototype predates F1-24..F1-27 — rendered here as plain
// buttons rather than a full dropdown-menu primitive (none exists in shared/ui yet and one screen
// does not justify adding it).
"use client";

import { useState } from "react";
import Link from "next/link";
import {
  initialsOfClassStudent,
  useClassStudents,
  useCreateClass,
  useCreateInviteCode,
  useDeleteClass,
  useInstructorClasses,
  useInviteCodes,
  useRemoveStudent,
  useUpdateClass,
  type ClassSummary,
  type StudentStatus,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  Modal,
  PageHeader,
  ProgressBar,
  SegmentedTabs,
  StatCard,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

// Trạng thái/màu theo 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:275-283 (Đang tốt/Cần hỗ trợ/
// Vắng bài), cùng bảng màu đã dùng ở class-progress-view.tsx cho model StudentStatus dùng chung.
const STATUS_VARIANT: Record<StudentStatus, BadgeVariant> = {
  insufficient_data: "neutral",
  absent: "negative",
  needs_support: "warn",
  watch: "blue",
  on_track: "success",
};

type ClassDialog = { mode: "create" } | { mode: "edit"; target: ClassSummary } | null;

export function ClassManagementView() {
  const t = useT("classManagement");
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  const [classTab, setClassTab] = useState<string>("all");
  const studentsQuery = useClassStudents(classTab === "all" ? undefined : classTab);

  const [formDialog, setFormDialog] = useState<ClassDialog>(null);
  const [inviteClassId, setInviteClassId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ClassSummary | null>(null);
  const [removeTarget, setRemoveTarget] = useState<{ id: string; name: string; classId: string } | null>(null);

  const createClass = useCreateClass();
  const updateClass = useUpdateClass();
  const deleteClass = useDeleteClass();
  const removeStudent = useRemoveStudent();

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0);
  const avgCompletion = classes.length
    ? Math.round(classes.reduce((sum, c) => sum + c.completionPct, 0) / classes.length)
    : 0;
  const pendingGrading = classes.reduce((sum, c) => sum + c.pendingGrading, 0);
  const absentStudents = classes.reduce((sum, c) => sum + c.absentCount, 0);

  const columns: DataTableColumn<NonNullable<typeof studentsQuery.data>[number]>[] = [
    {
      key: "name",
      header: t("studentList.col.studentName"),
      render: (row) => (
        <Link
          href={`/instructor/classes/${row.classId}/students/${row.id}`}
          className="flex min-w-0 items-center gap-2.5 font-semibold hover:underline"
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
    { key: "className", header: t("studentList.col.className"), render: (row) => row.className },
    {
      key: "avgScore",
      header: t("studentList.col.avgScore"),
      align: "right",
      render: (row) => <span className="font-mono">{row.avgScore === null ? "-" : row.avgScore.toFixed(1)}</span>,
    },
    {
      key: "completion",
      header: t("studentList.col.completion"),
      align: "right",
      render: (row) => `${row.completionPct}%`,
    },
    {
      key: "status",
      header: t("studentList.col.status"),
      render: (row) => <Badge variant={STATUS_VARIANT[row.status]}>{t(`status.${row.status}`)}</Badge>,
    },
    {
      key: "lastActive",
      header: t("studentList.col.lastActive"),
      align: "right",
      render: (row) => row.lastActiveLabel,
    },
    {
      key: "remove",
      header: "",
      align: "right",
      render: (row) => (
        <Button
          variant="ghost"
          size="sm"
          className="text-[var(--color-admin-negative)]"
          onClick={() => setRemoveTarget({ id: row.id, name: row.name, classId: row.classId })}
        >
          {t("btnRemoveStudent")}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("header.title")}
        description={t("header.summary", { classCount: classes.length, studentCount: totalStudents })}
        actions={
          <Button variant="cta" size="sm" onClick={() => setFormDialog({ mode: "create" })}>
            {t("header.btnCreateClass")}
          </Button>
        }
      />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]">
        <StatCard label={t("stats.classCount")} value={classes.length} meta={t("stats.classCountMeta", { count: totalStudents })} />
        <StatCard label={t("stats.avgCompletion")} value={`${avgCompletion}%`} />
        <StatCard label={t("stats.pendingGrading")} value={pendingGrading} />
        <StatCard label={t("stats.absentStudents")} value={absentStudents} />
      </div>

      <div className="mb-4 grid grid-cols-1 gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]">
        {classesQuery.isLoading
          ? Array.from({ length: 3 }, (_, i) => <Card key={i} className="h-40 animate-pulse" title="">{null}</Card>)
          : classes.map((klass) => (
              <Card
                key={klass.id}
                title={klass.name}
                description={klass.scheduleNote}
                action={<Badge variant="blue">{t("classList.col.studentCount", { count: klass.studentCount })}</Badge>}
              >
                <ProgressBar value={klass.completionPct} label={t("classList.col.completionBar")} className="mb-2" />
                <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">{klass.completionPct}%</p>
                <div className="mb-3 flex gap-4 text-[12.5px] text-[var(--color-text-muted)]">
                  <span>{t("classList.col.avgScore")}: {klass.avgScore ?? "-"}</span>
                  <span>{t("classList.col.pendingGrading")}: {klass.pendingGrading}</span>
                  <span>{t("classList.col.absentCount")}: {klass.absentCount}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="ghost" size="sm" onClick={() => setClassTab(klass.id)}>
                    {t("btnFilterClass")}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setFormDialog({ mode: "edit", target: klass })}>
                    {t("classList.col.menuEdit")}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setInviteClassId(klass.id)}>
                    {t("classList.col.menuInvite")}
                  </Button>
                  <Button variant="ghost" size="sm" className="text-[var(--color-admin-negative)]" onClick={() => setDeleteTarget(klass)}>
                    {t("classList.col.menuDelete")}
                  </Button>
                </div>
              </Card>
            ))}
      </div>

      <Card title={t("studentList.title")}>
        <div className="mb-3">
          <SegmentedTabs
            label={t("studentList.title")}
            value={classTab}
            onValueChange={setClassTab}
            options={[{ value: "all", label: t("filterAll") }, ...classes.map((c) => ({ value: c.id, label: c.name }))]}
          />
        </div>
        <DataTable
          caption={t("studentList.title")}
          columns={columns}
          rows={studentsQuery.data ?? []}
          rowKey={(row) => row.id}
          status={studentsQuery.isLoading ? "loading" : studentsQuery.isError ? "error" : "ready"}
          emptyMessage={t("emptyStudents")}
          minWidth={700}
        />
      </Card>

      <ClassFormDialog
        dialog={formDialog}
        onClose={() => setFormDialog(null)}
        onSubmit={(form) => {
          if (formDialog?.mode === "edit") updateClass.mutate({ id: formDialog.target.id, form });
          else createClass.mutate(form);
          setFormDialog(null);
        }}
        t={t}
      />

      <InviteCodeDialog classId={inviteClassId} onClose={() => setInviteClassId(null)} t={t} />

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteClass.mutate(deleteTarget.id);
          setDeleteTarget(null);
        }}
        title={t("popup.deleteClassTitle", { name: deleteTarget?.name ?? "" })}
        confirmLabel={t("popup.deleteClassConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("popup.deleteClassBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget) removeStudent.mutate(removeTarget.id);
          setRemoveTarget(null);
        }}
        title={t("popup.removeStudentTitle", { name: removeTarget?.name ?? "" })}
        confirmLabel={t("popup.removeStudentConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("popup.removeStudentBody")}
      </ConfirmDialog>
    </div>
  );
}

function ClassFormDialog({
  dialog,
  onClose,
  onSubmit,
  t,
}: {
  dialog: ClassDialog;
  onClose: () => void;
  onSubmit: (form: { name: string; description?: string; scheduleNote?: string }) => void;
  t: ReturnType<typeof useT>;
}) {
  const editing = dialog?.mode === "edit" ? dialog.target : null;
  const [name, setName] = useState(editing?.name ?? "");
  const [scheduleNote, setScheduleNote] = useState(editing?.scheduleNote ?? "");

  if (!dialog) return null;

  return (
    <Modal
      open
      onClose={onClose}
      title={dialog.mode === "create" ? t("popup.createClassTitle") : t("popup.editClassTitle")}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            {t("cancel")}
          </Button>
          <Button size="sm" disabled={!name.trim()} onClick={() => onSubmit({ name: name.trim(), scheduleNote: scheduleNote.trim() || undefined })}>
            {t("popup.saveClass")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <TextField label={t("popup.classNameLabel")} value={name} onChange={(e) => setName(e.target.value)} required />
        <TextField label={t("popup.scheduleLabel")} value={scheduleNote} onChange={(e) => setScheduleNote(e.target.value)} />
      </div>
    </Modal>
  );
}

function InviteCodeDialog({ classId, onClose, t }: { classId: string | null; onClose: () => void; t: ReturnType<typeof useT> }) {
  const invitesQuery = useInviteCodes(classId ?? "", classId !== null);
  const createInvite = useCreateInviteCode();

  if (!classId) return null;

  return (
    <Modal
      open
      onClose={onClose}
      title={t("popup.inviteCodeTitle")}
      footer={
        <Button size="sm" onClick={() => createInvite.mutate(classId)}>
          {t("popup.createInviteCode")}
        </Button>
      }
    >
      <ul className="flex flex-col gap-2">
        {(invitesQuery.data ?? []).map((invite) => (
          <li key={invite.id} className="flex items-center justify-between rounded-lg border border-[var(--color-border)] px-3 py-2">
            <span className="font-mono font-semibold">{invite.code}</span>
            <span className="text-[12px] text-[var(--color-text-muted)]">{invite.expiresAtLabel}</span>
          </li>
        ))}
      </ul>
    </Modal>
  );
}
