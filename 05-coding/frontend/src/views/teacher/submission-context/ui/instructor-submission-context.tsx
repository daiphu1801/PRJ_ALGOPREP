// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Context of the read-only submission page for A2 (DEC-2026-1013-instructor-avatar-menu-and-read-source):
// WHO submitted (student, class) and where to go next (open the student profile).
// The calling screen travels in `?from=submissions|student|class` (default: the submission list) and drives
// the breadcrumb and the lit sidebar item; the student and class travel as `?studentId=&classId=` hints
// (DEC-2026-1014-instructor-submissions-list-details). A hint that does not match a learner of that class
// (a learner who left still counts) is ignored: the page then shows the plain read-only note.
// ponytail: the mock submissions are shared by several learners, so the owner cannot be read from the
// submission itself as the real server will; the hint is only checked against the class, see PROTOTYPE_DEBT.
"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  initialsOfClassStudent,
  useClassStudentDetail,
  useInstructorClasses,
} from "@/entities/class";
import {
  parseSubmissionOrigin,
  type SubmissionOrigin,
} from "@/entities/submission";
import { useT } from "@/shared/i18n";
import { Badge } from "@/shared/ui";

type SubmissionRef = {
  id: string;
  problemTitle: string;
  submittedAt: string;
};

type Refs = { studentId: string; classId: string };

function useOrigin(): SubmissionOrigin {
  return parseSubmissionOrigin(useSearchParams().get("from"));
}

function useStudentRef(): Refs | null {
  const searchParams = useSearchParams();
  const studentId = searchParams.get("studentId");
  const classId = searchParams.get("classId");
  return studentId && classId ? { studentId, classId } : null;
}

/** Names the hint points at; `known` is false once the lookup finished and found no such learner in that class. */
function useResolved(ref: Refs) {
  const classes = useInstructorClasses();
  // A learner who left the class still owns the submission, so the lookup includes them.
  const student = useClassStudentDetail(ref.studentId, ref.classId, true);
  return {
    loading: student.isLoading,
    known: student.data != null,
    studentName: student.data?.name ?? null,
    className: classes.data?.find((c) => c.id === ref.classId)?.name ?? null,
  };
}

function ListCrumb({ submission }: { submission: SubmissionRef }) {
  const t = useT("instructorSubmission");
  const router = useRouter();
  const fromList = useSearchParams().get("from") === "submissions";
  return (
    <>
      <Link
        href="/instructor/submissions"
        className="hover:text-[var(--color-text)]"
        onClick={(event) => {
          // Opened from the list: go back in history so its filters, search and page are restored.
          if (fromList && window.history.length > 1) {
            event.preventDefault();
            router.back();
          }
        }}
      >
        {t("breadcrumb.submissions")}
      </Link>
      <span aria-hidden="true">/</span>
      <span>{t("breadcrumb.current", { id: submission.id })}</span>
    </>
  );
}

export function InstructorSubmissionBreadcrumb({
  submission,
}: {
  submission: SubmissionRef;
}) {
  const origin = useOrigin();
  const ref = useStudentRef();
  // student and class screens need the hint; without it every origin degrades to the list.
  if (!ref || origin === "submissions")
    return <ListCrumb submission={submission} />;
  return (
    <ResolvedBreadcrumb refs={ref} origin={origin} submission={submission} />
  );
}

function ResolvedBreadcrumb({
  refs,
  origin,
  submission,
}: {
  refs: Refs;
  origin: Exclude<SubmissionOrigin, "submissions">;
  submission: SubmissionRef;
}) {
  const t = useT("instructorSubmission");
  const { loading, known, studentName, className } = useResolved(refs);
  if (loading) return null;
  if (!known) return <ListCrumb submission={submission} />;
  return (
    <>
      <Link
        href="/instructor/classes"
        className="hover:text-[var(--color-text)]"
      >
        {t("breadcrumb.classes")}
      </Link>
      <span aria-hidden="true">/</span>
      <Link
        href={`/instructor/classes/${refs.classId}`}
        className="hover:text-[var(--color-text)]"
      >
        {className ?? refs.classId}
      </Link>
      {origin === "student" ? (
        <>
          <span aria-hidden="true">/</span>
          <Link
            href={`/instructor/classes/${refs.classId}/students/${refs.studentId}`}
            className="hover:text-[var(--color-text)]"
          >
            {studentName ?? refs.studentId}
          </Link>
        </>
      ) : null}
      <span aria-hidden="true">/</span>
      <span>{t("breadcrumb.current", { id: submission.id })}</span>
    </>
  );
}

export function InstructorSubmissionContext({
  submission,
}: {
  submission: SubmissionRef;
}) {
  const ref = useStudentRef();
  return ref ? (
    <ResolvedContext refs={ref} submission={submission} />
  ) : (
    <ContextCard submission={submission} />
  );
}

function ResolvedContext({
  refs,
  submission,
}: {
  refs: { studentId: string; classId: string };
  submission: SubmissionRef;
}) {
  const { loading, known, studentName, className } = useResolved(refs);
  if (loading) return null;
  // A hint that names nobody of that class is not shown as if it were true.
  if (!known) return <ContextCard submission={submission} />;
  return (
    <ContextCard
      submission={submission}
      refs={refs}
      studentName={studentName}
      className={className}
    />
  );
}

function ContextCard({
  submission,
  refs,
  studentName,
  className,
}: {
  submission: SubmissionRef;
  refs?: { studentId: string; classId: string };
  studentName?: string | null;
  className?: string | null;
}) {
  const t = useT("instructorSubmission");
  return (
    <section
      aria-label={t("contextLabel")}
      className="glass-card mb-4 flex flex-wrap items-center gap-x-4 gap-y-3 border border-[var(--color-border)] px-5 py-3.5"
    >
      {refs ? (
        <div className="flex min-w-0 items-center gap-3">
          <span
            aria-hidden="true"
            className="glass-surface flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[var(--color-border)] text-sm font-semibold"
          >
            {studentName ? initialsOfClassStudent(studentName) : "?"}
          </span>
          <div className="min-w-0">
            <p className="truncate text-[15px] font-semibold">
              <Link
                href={`/instructor/classes/${refs.classId}/students/${refs.studentId}`}
                className="hover:underline"
              >
                {studentName ?? refs.studentId}
              </Link>
              <span className="font-normal text-[var(--color-text-muted)]">
                {" · "}
                {submission.problemTitle}
              </span>
            </p>
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              <Link
                href={`/instructor/classes/${refs.classId}`}
                className="hover:underline"
              >
                {className ?? refs.classId}
              </Link>
            </p>
          </div>
        </div>
      ) : (
        <p className="text-[15px] font-semibold">{submission.problemTitle}</p>
      )}
      <Badge variant="neutral">{t("readOnlyBadge")}</Badge>
      <p className="min-w-[200px] flex-1 text-[12.5px] text-[var(--color-text-muted)]">
        {t("readOnlyNote")}
      </p>
    </section>
  );
}
