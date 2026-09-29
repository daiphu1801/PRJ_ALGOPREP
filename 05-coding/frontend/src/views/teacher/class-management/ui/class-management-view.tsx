// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// CLASS management only — restructured 2026-09-27 (owner: "1 pages quản lý lớp học, 1 pages quản
// lý danh sách học viên"). The "Danh sách học viên" table the mockup puts here (:159-188) moved to
// class_progress, which already listed the same students with progress columns; keeping both meant
// two near-identical tables over one dataset. "Xem học viên" now links there with ?classId= so the
// per-class view is one click away.
//
// Layout theo 09-layoutBase/Giáo viên - Lớp của tôi.dc.html: header + nút "+ Tạo lớp mới"
// (:115-121), lưới thẻ lớp (:136-157, dữ liệu :260-265).
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
  useCreateClass,
  useCreateInviteCode,
  useDeleteClass,
  useInstructorClasses,
  useInviteCodes,
  useUpdateClass,
  type ClassSummary,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  Modal,
  PageHeader,
  ProgressBar,
  TextField,
} from "@/shared/ui";

type ClassDialog = { mode: "create" } | { mode: "edit"; target: ClassSummary } | null;

export function ClassManagementView() {
  const t = useT("classManagement");
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];


  const [formDialog, setFormDialog] = useState<ClassDialog>(null);
  const [inviteClassId, setInviteClassId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ClassSummary | null>(null);

  const createClass = useCreateClass();
  const updateClass = useUpdateClass();
  const deleteClass = useDeleteClass();

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0);

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

      {/* Stat strip dropped 2026-09-27 (owner): every figure on it — số lớp, tổng học viên, cần
          chấm tay, điểm TB — is already the stat strip of instructor_overview, and repeating it on
          each screen pushed the actual worklist below the fold. These screens now open straight on
          their list + filter bar. Deliberate divergence from the mockup's own per-screen strip. */}
      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]">
        {classesQuery.isLoading
          ? Array.from({ length: 3 }, (_, i) => <Card key={i} className="h-40 animate-pulse" title="">{null}</Card>)
          : classes.map((klass) => (
              <Card
                key={klass.id}
                title={klass.name}
                description={klass.scheduleNote}
                action={<Badge variant="blue">{t("classList.col.studentCount", { count: klass.studentCount })}</Badge>}
              >
                <ProgressBar fill="var(--instructor-progress-fill, var(--color-admin-teal))" value={klass.completionPct} label={t("classList.col.completionBar")} className="mb-2" />
                <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">{klass.completionPct}%</p>
                <div className="mb-3 flex gap-4 text-[12.5px] text-[var(--color-text-muted)]">
                  <span>{t("classList.col.avgScore")}: {klass.avgScore ?? "-"}</span>
                  <span>{t("classList.col.pendingGrading")}: {klass.pendingGrading}</span>
                  <span>{t("classList.col.absentCount")}: {klass.absentCount}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="ghost" size="sm" asChild>
                    <Link href={`/instructor/students?classId=${klass.id}`}>{t("btnViewStudents")}</Link>
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
