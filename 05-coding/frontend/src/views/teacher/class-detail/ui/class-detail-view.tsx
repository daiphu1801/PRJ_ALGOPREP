// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Class detail page, INS0205 `class_detail` (owner 2026-10-11, reversing the 2026-08-28 answer in RD
// INS0201 Q3 that a child page was not needed: "3 nút với popup không cung cấp đủ thông tin").
// One page for what used to be spread over popups and filters: the class header, the
// structured schedule, the invite codes (list + create, F1-25) and the students of this class with
// the remove action (F1-26). The figure strip (students, completion, average, pending, absent) was
// dropped 2026-10-11 by the owner: those numbers already show in the class list. The create/edit popup is the shared `features/edit-class` dialog.
//
// No 09-layoutBase mockup exists for this screen; the layout follows the other instructor list and
// detail screens. [SoT: Suy luận]
"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Pencil, Trash2, UserMinus } from "lucide-react";
import {
  formatClassSchedule,
  initialsOfClassStudent,
  useClassStudents,
  useCreateInviteCode,
  useDeleteClass,
  useInstructorClasses,
  useInviteCodes,
  useRemoveStudent,
  useUpdateClass,
  type ClassSchedule,
  type ClassStudent,
  STUDENT_STATUS_VARIANT,
} from "@/entities/class";
import { ClassFormDialog, type ClassDialogState } from "@/features/edit-class";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  EmptyState,
  IconAction,
  PageHeader,
  QueryError,
  Skeleton,
  type DataTableColumn,
} from "@/shared/ui";

const dayMonthYear = (iso: string) => {
  const [year, month, day] = iso.split("-");
  return year && month && day ? `${day}/${month}/${year}` : iso;
};

export function ClassDetailView({ classId }: { classId: string }) {
  const t = useT("classDetail");
  const tm = useT("classManagement");
  const router = useRouter();

  const classesQuery = useInstructorClasses();
  const klass = (classesQuery.data ?? []).find((c) => c.id === classId);
  const studentsQuery = useClassStudents(classId);
  const invitesQuery = useInviteCodes(classId, true);

  const updateClass = useUpdateClass();
  const deleteClass = useDeleteClass();
  const createInvite = useCreateInviteCode();
  const removeStudent = useRemoveStudent();

  const [formDialog, setFormDialog] = useState<ClassDialogState>(null);
  const [deleting, setDeleting] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<ClassStudent | null>(null);

  const back = (
    <IconAction
      icon={ArrowLeft}
      label={t("backToList")}
      href="/instructor/classes"
    />
  );

  if (classesQuery.isLoading) {
    return (
      <div>
        <PageHeader leading={back} title={<Skeleton className="h-6 w-48" />} />
        <Skeleton className="h-40" />
      </div>
    );
  }
  if (classesQuery.isError) return <QueryError query={classesQuery} />;
  if (!klass) {
    return (
      <div>
        <PageHeader leading={back} title={t("notFoundTitle")} />
        <EmptyState>{t("notFound")}</EmptyState>
      </div>
    );
  }

  const dayLabel = (day: number) => tm(`schedule.dayShort.${day}`);
  const summary = formatClassSchedule(klass.schedule, dayLabel);
  const studentColumns: DataTableColumn<ClassStudent>[] = [
    {
      key: "name",
      header: t("students.col.name"),
      render: (row) => (
        <Link
          href={`/instructor/classes/${row.classId}/students/${row.id}`}
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
      key: "status",
      header: t("students.col.status"),
      render: (row) => (
        <Badge variant={STUDENT_STATUS_VARIANT[row.status]}>
          {t(`status.${row.status}`)}
        </Badge>
      ),
    },
    {
      key: "avgScore",
      header: t("students.col.avgScore"),
      align: "right",
      render: (row) => (row.avgScore === null ? "-" : row.avgScore.toFixed(1)),
    },
    {
      key: "completion",
      header: t("students.col.completion"),
      align: "right",
      render: (row) => `${row.completionPct}%`,
    },
    {
      key: "lastActive",
      header: t("students.col.lastActive"),
      render: (row) => row.lastActiveLabel,
    },
    {
      key: "remove",
      header: "",
      align: "right",
      render: (row) => (
        <IconAction
          icon={UserMinus}
          label={t("students.remove")}
          ariaLabel={`${t("students.remove")} ${row.name}`}
          tone="danger"
          onClick={() => setRemoveTarget(row)}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        leading={back}
        title={klass.name}
        description={klass.description || t("noDescription")}
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => setFormDialog({ mode: "edit", target: klass })}
            >
              <Pencil className="mr-1.5 h-3.5 w-3.5" aria-hidden="true" />
              {t("edit")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)] text-[var(--color-admin-negative)]"
              onClick={() => setDeleting(true)}
            >
              <Trash2 className="mr-1.5 h-3.5 w-3.5" aria-hidden="true" />
              {t("delete")}
            </Button>
          </>
        }
      />

      <div className="mb-4 grid grid-cols-1 items-start gap-3.5 lg:grid-cols-2">
        <Card title={t("schedule.title")} description={summary || undefined}>
          <ScheduleDetails
            schedule={klass.schedule}
            t={t}
            dayLabel={dayLabel}
          />
        </Card>

        <Card
          title={t("invite.title")}
          description={t("invite.subtitle")}
          action={
            <Button
              variant="cta"
              size="sm"
              onClick={() =>
                createInvite.mutate(classId, {
                  onSuccess: () => toast.success(tm("toast.inviteCreated")),
                  onError: () => toast.error(tm("toast.failed")),
                })
              }
            >
              {t("invite.create")}
            </Button>
          }
        >
          {invitesQuery.isLoading ? (
            <Skeleton className="h-16" />
          ) : (invitesQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">
              {t("invite.empty")}
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {(invitesQuery.data ?? []).map((invite) => (
                <li
                  key={invite.id}
                  className="flex items-center justify-between rounded-lg border border-[var(--color-border)] px-3 py-2"
                >
                  <span className="font-mono font-semibold">{invite.code}</span>
                  <span className="text-[12px] text-[var(--color-text-muted)]">
                    {invite.expiresAtLabel}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card
        title={t("students.title")}
        description={t("students.subtitle", { count: klass.studentCount })}
        action={
          <Button variant="ghost" size="sm" asChild>
            <Link href={`/instructor/students?classId=${klass.id}`}>
              {t("students.openFull")}
            </Link>
          </Button>
        }
      >
        <DataTable
          caption={t("students.title")}
          columns={studentColumns}
          rows={studentsQuery.data ?? []}
          rowKey={(row) => row.id}
          status={
            studentsQuery.isLoading
              ? "loading"
              : studentsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("students.empty")}
          minWidth={720}
        />
      </Card>

      <ClassFormDialog
        key={formDialog?.mode === "edit" ? formDialog.target.id : "closed"}
        dialog={formDialog}
        onClose={() => setFormDialog(null)}
        onSubmit={(form) => {
          updateClass.mutate(
            { id: classId, form },
            {
              onSuccess: () =>
                toast.success(tm("toast.updated", { name: form.name })),
              onError: () => toast.error(tm("toast.failed")),
            },
          );
          setFormDialog(null);
        }}
        t={tm}
      />

      <ConfirmDialog
        open={deleting}
        onClose={() => setDeleting(false)}
        onConfirm={() => {
          const name = klass.name;
          deleteClass.mutate(classId, {
            onSuccess: () => {
              toast.success(tm("toast.deleted", { name }));
              router.push("/instructor/classes");
            },
            onError: () => toast.error(tm("toast.failed")),
          });
          setDeleting(false);
        }}
        title={tm("popup.deleteClassTitle", { name: klass.name })}
        confirmLabel={tm("popup.deleteClassConfirm")}
        cancelLabel={tm("cancel")}
        destructive
      >
        {tm("popup.deleteClassBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget) {
            removeStudent.mutate(removeTarget.id, {
              onSuccess: () =>
                toast.success(
                  t("toast.studentRemoved", { name: removeTarget.name }),
                ),
              onError: () => toast.error(tm("toast.failed")),
            });
          }
          setRemoveTarget(null);
        }}
        title={tm("popup.removeStudentTitle", {
          name: removeTarget?.name ?? "",
        })}
        confirmLabel={tm("popup.removeStudentConfirm")}
        cancelLabel={tm("cancel")}
        destructive
      >
        {tm("popup.removeStudentBody")}
      </ConfirmDialog>
    </div>
  );
}

/** The schedule read part by part (the card subtitle already has the one-line form). */
function ScheduleDetails({
  schedule,
  t,
  dayLabel,
}: {
  schedule: ClassSchedule | undefined;
  t: ReturnType<typeof useT>;
  dayLabel: (day: number) => string;
}) {
  const rows: [string, string][] = [];
  if (schedule) {
    if (schedule.days.length > 0)
      rows.push([
        t("schedule.days"),
        [...schedule.days]
          .sort((a, b) => a - b)
          .map(dayLabel)
          .join(", "),
      ]);
    if (schedule.startTime)
      rows.push([
        t("schedule.time"),
        schedule.endTime
          ? `${schedule.startTime} – ${schedule.endTime}`
          : schedule.startTime,
      ]);
    if (schedule.room) rows.push([t("schedule.room"), schedule.room]);
    if (schedule.startDate)
      rows.push([
        t("schedule.period"),
        schedule.endDate
          ? `${dayMonthYear(schedule.startDate)} – ${dayMonthYear(schedule.endDate)}`
          : dayMonthYear(schedule.startDate),
      ]);
    if (schedule.note) rows.push([t("schedule.note"), schedule.note]);
  }
  if (rows.length === 0)
    return (
      <p className="text-sm text-[var(--color-text-muted)]">
        {t("schedule.empty")}
      </p>
    );
  return (
    <dl className="flex flex-col text-sm">
      {rows.map(([label, value]) => (
        <div
          key={label}
          className="flex items-start justify-between gap-4 border-b border-[var(--color-border)] py-2 last:border-0"
        >
          <dt className="text-[var(--color-text-muted)]">{label}</dt>
          <dd className="text-right font-medium">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
