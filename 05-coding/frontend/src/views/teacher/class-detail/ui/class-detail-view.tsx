// PROTOTYPE with a real API behind it (task 12, 03-dd/api/F1_identity.md #19, #21 to #24, #26, #27);
// see 06-plan/PROTOTYPE_DEBT.md. In real mode the roster shows the contact details the server has and
// leaves out status, average, completion and last activity: they need F4 metrics (not built).
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

import { UserAvatar } from "@/entities/user";
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  CalendarDays,
  Copy,
  Pencil,
  StickyNote,
  Trash2,
  UserMinus,
} from "lucide-react";
import {
  useClassRoster,
  useCreateInviteCode,
  useDeleteClass,
  useInstructorClasses,
  useInviteCodes,
  useRemoveStudent,
  useRevokeInviteCode,
  type InviteCode,
  useUpdateClass,
  type ClassRosterRow,
  type ClassSchedule,
  STUDENT_STATUS_VARIANT,
} from "@/entities/class";
import {
  ClassFormDialog,
  DeleteClassNotice,
  type ClassDialogState,
} from "@/features/edit-class";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  EmptyState,
  IconAction,
  PageContainer,
  PageHeader,
  QueryError,
  SeeAllLink,
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
  const toMessage = useApiErrorMessage();
  const router = useRouter();

  const classesQuery = useInstructorClasses();
  const klass = (classesQuery.data ?? []).find((c) => c.id === classId);
  const studentsQuery = useClassRoster(classId);
  const invitesQuery = useInviteCodes(classId, true);

  const updateClass = useUpdateClass();
  const deleteClass = useDeleteClass();
  const createInvite = useCreateInviteCode();
  const removeStudent = useRemoveStudent();

  const [formDialog, setFormDialog] = useState<ClassDialogState>(null);
  const [deleting, setDeleting] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<ClassRosterRow | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<InviteCode | null>(null);
  const revokeInvite = useRevokeInviteCode();

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

  async function copyInvite(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      toast.success(t("invite.copied"));
    } catch {
      toast.error(t("invite.copyFailed"));
    }
  }

  // The judge figures come with the roster when the server could read them (F1 #26).
  const hasMetrics = (studentsQuery.data ?? []).some((row) => row.metrics);
  const metricColumns: DataTableColumn<ClassRosterRow>[] = [
    {
      key: "status",
      header: t("students.col.status"),
      render: (row) =>
        row.metrics ? (
          <Badge variant={STUDENT_STATUS_VARIANT[row.metrics.status]}>
            {t(`status.${row.metrics.status}`)}
          </Badge>
        ) : null,
    },
    {
      key: "avgScore",
      header: t("students.col.avgScore"),
      align: "right",
      render: (row) =>
        !row.metrics || row.metrics.avgScore === null
          ? "-"
          : row.metrics.avgScore.toFixed(1),
    },
    {
      key: "completion",
      header: t("students.col.completion"),
      align: "right",
      render: (row) =>
        row.metrics
          ? row.metrics.completionPct === null
            ? "-"
            : `${row.metrics.completionPct}%`
          : "",
    },
    {
      key: "lastActive",
      header: t("students.col.lastActive"),
      render: (row) => row.metrics?.lastActiveLabel ?? "",
    },
  ];
  const contactColumns: DataTableColumn<ClassRosterRow>[] = [
    {
      key: "email",
      header: t("students.col.email"),
      render: (row) => row.email ?? "-",
    },
    {
      key: "phone",
      header: t("students.col.phone"),
      render: (row) => row.phone ?? "-",
    },
    {
      key: "joinedAt",
      header: t("students.col.joinedAt"),
      render: (row) => (row.joinedAt ? formatDateVN(row.joinedAt) : "-"),
    },
  ];
  const studentColumns: DataTableColumn<ClassRosterRow>[] = [
    {
      key: "name",
      header: t("students.col.name"),
      render: (row) => (
        <Link
          href={`/instructor/classes/${row.classId}/students/${row.id}`}
          className="flex items-center gap-2.5 font-semibold hover:underline"
        >
          <UserAvatar
            userId={row.id}
            name={row.name}
            avatarVersion={row.avatarVersion}
            size="sm"
          />
          {row.name}
          {row.accountInactive ? (
            <Badge variant="neutral">{t("students.inactive")}</Badge>
          ) : null}
        </Link>
      ),
    },
    // Contact details always show; the judge figures join them when the server could read them.
    ...contactColumns,
    ...(hasMetrics ? metricColumns : []),
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
    <PageContainer variant="content" className="@container">
      <PageHeader
        leading={back}
        title={
          <span className="block whitespace-normal [overflow-wrap:anywhere]">
            {klass.name}
          </span>
        }
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

      <div className="mb-4 grid min-w-0 grid-cols-1 items-start gap-3.5 @min-[880px]:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] @min-[880px]:grid-rows-[auto_1fr]">
        <Card
          title={t("schedule.title")}
          className="@container min-w-0 @min-[880px]:col-start-1 @min-[880px]:row-span-2 @min-[880px]:row-start-1"
        >
          <ScheduleDetails schedule={klass.schedule} t={t} />
        </Card>

        <Card
          title={t("descriptionTitle")}
          className="min-w-0 @min-[880px]:col-start-2 @min-[880px]:row-start-2"
        >
          <p className="max-w-[72ch] text-sm leading-relaxed whitespace-pre-line [overflow-wrap:anywhere] text-[var(--color-text-muted)]">
            {klass.description || t("noDescription")}
          </p>
        </Card>

        <Card
          title={t("invite.title")}
          description={t("invite.subtitle")}
          className="min-w-0 @min-[880px]:col-start-2 @min-[880px]:row-start-1"
          action={
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() =>
                createInvite.mutate(classId, {
                  onSuccess: () => toast.success(tm("toast.inviteCreated")),
                  onError: (error) => toast.error(toMessage(error)),
                })
              }
            >
              {t("invite.create")}
            </Button>
          }
        >
          {invitesQuery.isLoading ? (
            <Skeleton className="h-16" />
          ) : invitesQuery.isError ? (
            <QueryError query={invitesQuery} />
          ) : (invitesQuery.data ?? []).length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">
              {t("invite.empty")}
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {(invitesQuery.data ?? []).map((invite) => (
                <li
                  key={invite.id}
                  className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 rounded-lg border border-[var(--color-border)] px-3 py-2"
                >
                  <div className="min-w-0">
                    <span className="block font-mono font-semibold [overflow-wrap:anywhere]">
                      {invite.code}
                    </span>
                    <span className="block text-xs text-[var(--color-text-muted)]">
                      {invite.expiresAtLabel}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="gap-1.5 border border-[var(--color-border)]"
                      aria-label={t("invite.copyCode", { code: invite.code })}
                      onClick={() => void copyInvite(invite.code)}
                    >
                      <Copy className="size-3.5" aria-hidden="true" />
                      {t("invite.copy")}
                    </Button>
                    <IconAction
                      icon={Trash2}
                      label={t("invite.revokeCode", { code: invite.code })}
                      tone="danger"
                      onClick={() => setRevokeTarget(invite)}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card
        className="grow"
        title={t("students.title")}
        description={t("students.subtitle", { count: klass.studentCount })}
        action={
          <SeeAllLink
            href={`/instructor/students?classId=${klass.id}`}
            className="mt-0"
          >
            {t("students.openFull")}
          </SeeAllLink>
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
          minWidth={0}
        />
      </Card>

      <ClassFormDialog
        key={formDialog?.mode === "edit" ? formDialog.target.id : "closed"}
        dialog={formDialog}
        onClose={() => setFormDialog(null)}
        onSubmit={async (form) => {
          try {
            await updateClass.mutateAsync({ id: classId, form });
            toast.success(tm("toast.updated", { name: form.name }));
          } catch (error) {
            // The dialog stays open with what was typed; the reason comes from the error code.
            toast.error(toMessage(error));
            throw error;
          }
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
            onError: (error) => toast.error(toMessage(error)),
          });
          setDeleting(false);
        }}
        title={tm("popup.deleteClassTitle", { name: klass.name })}
        confirmLabel={tm("popup.deleteClassConfirm")}
        cancelLabel={tm("cancel")}
        destructive
      >
        {deleting ? (
          <DeleteClassNotice
            classId={classId}
            studentCount={klass.studentCount}
          />
        ) : null}
      </ConfirmDialog>

      <ConfirmDialog
        open={revokeTarget !== null}
        onClose={() => setRevokeTarget(null)}
        onConfirm={() => {
          if (revokeTarget) {
            revokeInvite.mutate(
              { classId, codeId: revokeTarget.id },
              {
                onSuccess: () => toast.success(tm("toast.inviteRevoked")),
                onError: (error) => toast.error(toMessage(error)),
              },
            );
          }
          setRevokeTarget(null);
        }}
        title={t("invite.revokeTitle", { code: revokeTarget?.code ?? "" })}
        confirmLabel={t("invite.revokeConfirm")}
        cancelLabel={tm("cancel")}
        destructive
      >
        {t("invite.revokeBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget) {
            removeStudent.mutate(
              { classId, studentId: removeTarget.id },
              {
                onSuccess: () =>
                  toast.success(
                    t("toast.studentRemoved", { name: removeTarget.name }),
                  ),
                onError: (error) => toast.error(toMessage(error)),
              },
            );
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
    </PageContainer>
  );
}

function ScheduleDetails({
  schedule,
  t,
}: {
  schedule: ClassSchedule | undefined;
  t: ReturnType<typeof useT>;
}) {
  const days = [...(schedule?.days ?? [])]
    .sort((a, b) => a - b)
    .map((day) => t(`schedule.weekday.${day}`))
    .join(", ");
  const time = [schedule?.startTime, schedule?.endTime]
    .filter(Boolean)
    .join(" – ");
  const period = [schedule?.startDate, schedule?.endDate]
    .flatMap((date) => (date ? [dayMonthYear(date)] : []))
    .join(" – ");
  const meeting = [
    [t("schedule.days"), days],
    [t("schedule.time"), time],
  ].filter(([, value]) => value);
  const details = [
    [t("schedule.room"), schedule?.room],
    [t("schedule.period"), period],
  ].filter(([, value]) => value);

  return (
    <div className="space-y-4">
      {meeting.length > 0 && (
        <div className="flex items-start gap-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-border)]/25 p-3">
          <CalendarDays
            className="mt-0.5 size-4 shrink-0 text-[var(--color-text-muted)]"
            aria-hidden="true"
          />
          <dl className="grid min-w-0 flex-1 gap-x-5 gap-y-2 @min-[360px]:grid-cols-2">
            {meeting.map(([label, value]) => (
              <div key={label}>
                <dt className="text-xs text-[var(--color-text-muted)]">
                  {label}
                </dt>
                <dd className="mt-0.5 text-base font-semibold tabular-nums">
                  {value}
                </dd>
              </div>
            ))}
          </dl>
        </div>
      )}
      {details.length > 0 && (
        <dl className="grid gap-x-5 gap-y-3 text-sm @min-[360px]:grid-cols-2">
          {details.map(([label, value]) => (
            <div key={label} className="min-w-0">
              <dt className="text-xs text-[var(--color-text-muted)]">
                {label}
              </dt>
              <dd className="mt-0.5 whitespace-pre-line [overflow-wrap:anywhere]">
                {value}
              </dd>
            </div>
          ))}
        </dl>
      )}
      {meeting.length === 0 && details.length === 0 && (
        <p className="text-sm text-[var(--color-text-muted)]">
          {t("schedule.empty")}
        </p>
      )}
      {schedule?.note && (
        <section className="border-l-2 border-[var(--color-border)] pl-3">
          <h3 className="flex items-center gap-2 text-sm font-semibold">
            <StickyNote
              className="size-4 text-[var(--color-text-muted)]"
              aria-hidden="true"
            />
            {t("schedule.note")}
          </h3>
          <p className="mt-1 max-w-[72ch] text-sm leading-relaxed whitespace-pre-line [overflow-wrap:anywhere]">
            {schedule.note}
          </p>
        </section>
      )}
    </div>
  );
}
