// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Mock rows of the instructor submission list: every learner of the instructor's classes gets two of
// the readable mock submissions (entities/submission), newest first. A real endpoint would return
// the same shape from judge-orchestration, filtered by the instructor's classes.
import type { ClassStudent } from "@/entities/class";
import {
  fetchSubmissionDetail,
  READABLE_SUBMISSION_IDS,
  SUBMISSION_LANGUAGE_LABEL,
  type SubmissionStatus,
} from "@/entities/submission";

export type SubmissionRow = {
  key: string;
  submissionId: string;
  studentId: string;
  studentName: string;
  classId: string;
  className: string;
  /** The learner left the class: the history stays, the row carries "Đã rời lớp" and no student link. */
  leftClass: boolean;
  problemTitle: string;
  language: string;
  status: SubmissionStatus;
  ratio: string;
  submittedAt: number;
};

const HOUR = 3_600_000;

const NOT_JUDGED: SubmissionStatus[] = [
  "PENDING",
  "COMPILING",
  "JUDGING",
  "SYSTEM_ERROR",
];
const isJudged = (status: SubmissionStatus) => !NOT_JUDGED.includes(status);

export function buildSubmissionRows(students: ClassStudent[]): SubmissionRow[] {
  const count = READABLE_SUBMISSION_IDS.length;
  const rows = students.flatMap((student, k) =>
    [k % count, (k + 1) % count].flatMap((slot, n) => {
      const id = READABLE_SUBMISSION_IDS[slot]!;
      const detail = fetchSubmissionDetail(id);
      if (!detail) return [];
      return [
        {
          key: `${student.id}-${id}-${n}`,
          submissionId: id,
          studentId: student.id,
          studentName: student.name,
          classId: student.classId,
          className: student.className,
          leftClass: student.leftAt !== undefined,
          problemTitle: detail.problemTitle,
          language: SUBMISSION_LANGUAGE_LABEL[detail.language],
          status: detail.status,
          // Pass ratio (F4-13) only for a judged submission: "-" while it is not judged yet or ended by
          // a system error (DEC-2026-1014-instructor-submissions-list-rules).
          ratio:
            detail.totalCount == null || !isJudged(detail.status)
              ? "-"
              : `${detail.passedCount ?? 0}/${detail.totalCount}`,
          // Spread the learners over time so the list does not read as one block.
          submittedAt:
            new Date(detail.submittedAt).getTime() - (k * 5 + n * 2) * HOUR,
        },
      ];
    }),
  );
  return rows.sort((a, b) => b.submittedAt - a.submittedAt);
}
