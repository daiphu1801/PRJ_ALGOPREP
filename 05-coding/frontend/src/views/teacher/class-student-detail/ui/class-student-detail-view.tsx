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

import { useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Activity,
  ArrowLeft,
  GraduationCap,
  Mail,
  Phone,
  Eye,
  Tags,
  User,
  type LucideIcon,
} from "lucide-react";
import { useClassAssignments } from "@/entities/class-assignment";
import {
  matchesResultFilter,
  SUBMISSION_LANGUAGE_LABEL,
  SUBMISSION_STATUS_VARIANT,
  useInstructorReadableSubmissions,
  type SubmissionDetail,
  type SubmissionResultFilter,
} from "@/entities/submission";
import {
  STUDENT_STATUS_VARIANT,
  useClassStudentDetail,
  useInstructorClasses,
  useRemoveStudent,
} from "@/entities/class";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  FilterMenu,
  IconAction,
  PageHeader,
  QueryError,
  type DataTableColumn,
} from "@/shared/ui";

function InfoItem({
  icon: Icon,
  label,
  children,
}: {
  icon: LucideIcon;
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="flex items-start gap-3">
      <span
        aria-hidden="true"
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[color-mix(in_srgb,var(--color-primary)_12%,transparent)] text-[var(--color-primary)]"
      >
        <Icon className="h-4 w-4" />
      </span>
      <div className="min-w-0">
        <dt className="text-xs font-medium text-[var(--color-text-muted)]">
          {label}
        </dt>
        <dd className="mt-0.5 font-medium break-words text-[var(--color-text)]">
          {children}
        </dd>
      </div>
    </div>
  );
}

// Mock submission history — no dedicated entity yet (this screen has no
// 03-dd/api/F4_judge-orchestration.md contract to type a mock against). Rows ARE the mock submissions the
// instructor may read (entities/submission details, the same ids the instructor submission list uses), so
// language, verdict, testcase ratio and time here match what "Xem kết quả" opens.
function buildSubmissionHistory(readable: SubmissionDetail[]) {
  return readable.map((detail) => {
    const id = detail.id;
    return {
      id,
      submissionId: id,
      problemTitle: detail.problemTitle,
      language: SUBMISSION_LANGUAGE_LABEL[detail.language],
      status: detail.status,
      ratio:
        detail.totalCount == null
          ? "-"
          : `${detail.passedCount ?? 0}/${detail.totalCount}`,
      submittedAtLabel: new Date(detail.submittedAt).toLocaleString("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
      }),
    };
  });
}

export function ClassStudentDetailView({
  classId,
  studentId,
}: {
  classId: string;
  studentId: string;
}) {
  const t = useT("classStudentDetail");
  const classesQuery = useInstructorClasses();
  const studentQuery = useClassStudentDetail(studentId, classId);
  const assignmentsQuery = useClassAssignments();
  const removeStudent = useRemoveStudent();
  const router = useRouter();
  const [confirmingRemove, setConfirmingRemove] = useState(false);
  const tVerdict = useT("submissionResult");
  const [historyFilter, setHistoryFilter] =
    useState<SubmissionResultFilter>("all");

  const className =
    classesQuery.data?.find((c) => c.id === classId)?.name ?? classId;

  const classAssignments = useMemo(
    () =>
      (assignmentsQuery.data ?? []).filter((row) =>
        row.assignedClassIds.includes(classId),
      ),
    [assignmentsQuery.data, classId],
  );

  const topicBreakdown = useMemo(() => {
    const byTopic = new Map<string, number>();
    for (const row of classAssignments)
      byTopic.set(row.topic, (byTopic.get(row.topic) ?? 0) + 1);
    return [...byTopic.entries()];
  }, [classAssignments]);

  const readableQuery = useInstructorReadableSubmissions();
  const submissionHistory = useMemo(
    () => buildSubmissionHistory(readableQuery.data ?? []),
    [readableQuery.data],
  );

  const visibleHistory = useMemo(
    () =>
      submissionHistory.filter((row) =>
        matchesResultFilter(row.status, historyFilter),
      ),
    [submissionHistory, historyFilter],
  );

  const student = studentQuery.data;

  const assignmentColumns: DataTableColumn<
    (typeof classAssignments)[number]
  >[] = [
    {
      key: "title",
      header: t("assignmentProgress.col.title"),
      render: (row) => row.title,
    },
    {
      key: "topic",
      header: t("assignmentProgress.col.topic"),
      render: (row) => row.topic,
    },
    {
      key: "acRate",
      header: t("assignmentProgress.col.acRate"),
      align: "right",
      render: (row) => `${row.acRate}%`,
    },
  ];

  const historyColumns: DataTableColumn<(typeof submissionHistory)[number]>[] =
    [
      {
        key: "problem",
        header: t("submissionHistory.col.problem"),
        render: (row) => row.problemTitle,
      },
      {
        key: "language",
        header: t("submissionHistory.col.language"),
        render: (row) => <Badge variant="neutral">{row.language}</Badge>,
      },
      {
        key: "status",
        header: t("submissionHistory.col.status"),
        render: (row) => (
          <Badge variant={SUBMISSION_STATUS_VARIANT[row.status]}>
            {tVerdict(`verdict.${row.status}`)}
          </Badge>
        ),
      },
      {
        key: "ratio",
        header: t("submissionHistory.col.ratio"),
        align: "right",
        render: (row) => <span className="font-mono">{row.ratio}</span>,
      },
      {
        key: "submittedAt",
        header: t("submissionHistory.col.submittedAt"),
        render: (row) => row.submittedAtLabel,
      },
      {
        key: "action",
        header: "",
        align: "right",
        render: (row) => (
          <IconAction
            icon={Eye}
            label={t("submissionHistory.btnViewResult")}
            ariaLabel={t("submissionHistory.btnViewResultOf", {
              problem: row.problemTitle,
            })}
            href={`/instructor/submissions/${row.submissionId}?studentId=${studentId}&classId=${classId}&from=student`}
          />
        ),
      },
    ];

  if (studentQuery.isLoading) {
    return (
      <p className="p-6 text-sm text-[var(--color-text-muted)]">
        {t("loading")}
      </p>
    );
  }

  if (studentQuery.isError) {
    return (
      <div className="p-6">
        <QueryError query={studentQuery} />
      </div>
    );
  }

  if (!student) {
    return (
      <div className="p-6">
        <p className="text-sm text-[var(--color-text-muted)]">
          {t("notFound")}
        </p>
        <Link
          href={`/instructor/students?classId=${classId}`}
          className="mt-2 inline-block text-[var(--color-primary)] underline"
        >
          {t("backLink")}
        </Link>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        leading={
          <IconAction
            icon={ArrowLeft}
            label={t("backLink")}
            href={`/instructor/students?classId=${classId}`}
            // Back restores the list's own filter and page (INS0204 Q6); the href is the fallback
            // when this page was opened directly and there is nothing to go back to.
            onClick={(event) => {
              if (window.history.length > 1) {
                event.preventDefault();
                router.back();
              }
            }}
          />
        }
        title={student.name}
        description={
          <Link
            href={`/instructor/classes/${classId}`}
            className="hover:underline"
          >
            {className}
          </Link>
        }
        actions={
          <>
            <Button
              variant="ghost"
              size="sm"
              className="text-[var(--color-admin-negative)]"
              onClick={() => setConfirmingRemove(true)}
            >
              {t("btnRemoveFromClass")}
            </Button>
          </>
        }
      />

      <div className="mb-4 grid gap-4 lg:grid-cols-2">
        <Card title={t("info.contactTitle")} className="h-full">
          <dl className="flex flex-col gap-4 text-sm">
            <InfoItem icon={User} label={t("info.name")}>
              {student.name}
            </InfoItem>
            <InfoItem icon={Mail} label={t("info.email")}>
              {student.email ? (
                <a
                  href={`mailto:${student.email}`}
                  className="text-[var(--color-primary)] hover:underline"
                >
                  {student.email}
                </a>
              ) : (
                "-"
              )}
            </InfoItem>
            <InfoItem icon={Phone} label={t("info.phone")}>
              {student.phone ? (
                <a
                  href={`tel:${student.phone}`}
                  className="text-[var(--color-primary)] hover:underline"
                >
                  {student.phone}
                </a>
              ) : (
                "-"
              )}
            </InfoItem>
          </dl>
        </Card>

        <Card title={t("info.studyTitle")} className="h-full">
          <dl className="flex flex-col gap-4 text-sm">
            <InfoItem icon={GraduationCap} label={t("info.class")}>
              <Link
                href={`/instructor/classes/${classId}`}
                className="text-[var(--color-primary)] hover:underline"
              >
                {className}
              </Link>
            </InfoItem>
            <InfoItem icon={Activity} label={t("info.status")}>
              <Badge variant={STUDENT_STATUS_VARIANT[student.status]}>
                {t(`info.statusValue.${student.status}`)}
              </Badge>
            </InfoItem>
            <InfoItem icon={Tags} label={t("topicProgress.title")}>
              {topicBreakdown.length === 0 ? (
                "-"
              ) : (
                <ul className="flex flex-wrap gap-1.5">
                  {topicBreakdown.map(([topic, count]) => (
                    <li key={topic}>
                      <Badge variant="blue">
                        {t("topicProgress.item", { topic, count })}
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}
            </InfoItem>
          </dl>
        </Card>
      </div>

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
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <FilterMenu
            label={t("submissionHistory.filter.label")}
            value={historyFilter}
            onValueChange={(value) =>
              setHistoryFilter(value as SubmissionResultFilter)
            }
            options={[
              { value: "all", label: t("submissionHistory.filter.all") },
              {
                value: "accepted",
                label: t("submissionHistory.filter.accepted"),
              },
              { value: "failed", label: t("submissionHistory.filter.failed") },
            ]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("submissionHistory.resultCount", {
              count: visibleHistory.length,
            })}
          </span>
        </div>
        <DataTable
          caption={t("submissionHistory.title")}
          columns={historyColumns}
          rows={visibleHistory}
          rowKey={(row) => row.id}
          emptyMessage={t("empty")}
          minWidth={760}
        />
      </Card>

      <ConfirmDialog
        open={confirmingRemove}
        onClose={() => setConfirmingRemove(false)}
        onConfirm={() => {
          removeStudent.mutate(studentId, {
            onSuccess: () => {
              toast.success(t("toast.removed", { name: student.name }));
              // The learner left the class, so this page has nothing left to show: back to the caller
              // (BD INS0204 Q10, DEC-2026-1014-instructor-batch3-4-contracts-and-removal).
              router.back();
            },
            onError: () => toast.error(t("toast.failed")),
          });
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
