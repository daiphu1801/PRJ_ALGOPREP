// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Shared entity for the whole "cluster" of instructor class screens (class_management,
// class_assignments, class_progress, class_student_detail) — 02-bd/screens/teacher/
// INS0201_class_management.md:298-301 says the cluster's `entities/class` and
// `entities/class-student` must be defined once here, not re-declared per screen.
import { z } from "zod";

export const studentStatusSchema = z.enum([
  "insufficient_data",
  "absent",
  "needs_support",
  "watch",
  "on_track",
]);
export type StudentStatus = z.infer<typeof studentStatusSchema>;

// Structured class schedule (owner 2026-10-11: "phần lịch học nên chi tiết hơn"), replacing the free
// text `schedule_note`. Days are ISO weekdays, 1 = Monday ... 7 = Sunday; times are "HH:mm", dates
// "yyyy-mm-dd". Every part is optional, a class may have no schedule at all.
export const classScheduleSchema = z.object({
  days: z.array(z.number().int().min(1).max(7)),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  room: z.string().optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  note: z.string().optional(),
});
export type ClassSchedule = z.infer<typeof classScheduleSchema>;

// classManagement.classList.* (02-bd/screens/teacher/INS0201_class_management.md Sheet 5 Khu vực C).
export const classSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string().optional(),
  schedule: classScheduleSchema.optional(),
  studentCount: z.number(),
  // The three figures below need F4 metrics (F1 #56, not built): the real list (#19) leaves them out,
  // and a screen shows a column only when the data has it. Never a 0 standing in for "unknown".
  completionPct: z.number().optional(),
  avgScore: z.number().nullable().optional(),
  absentCount: z.number().optional(),
  createdAt: z.string(),
});
export type ClassSummary = z.infer<typeof classSummarySchema>;

// classManagement.studentList.* / classProgress.studentList.* — one row shared by both tables.
export const classStudentSchema = z.object({
  id: z.string(),
  name: z.string(),
  classId: z.string(),
  className: z.string(),
  avgScore: z.number().nullable(),
  completionPct: z.number(),
  streakDays: z.number(),
  trend: z.array(z.number()),
  lastActiveLabel: z.string(),
  status: studentStatusSchema,
  // Contact details the instructor uses to reach the student (owner, 2026-10-13). Optional until
  // the identity DD fixes which profile fields an A2 may read.
  email: z.string().optional(),
  phone: z.string().optional(),
  // Set when the instructor removes the learner: the enrolment row stays as history
  // (DEC-2026-1014-keep-learning-history-on-leave), the learner just leaves the active lists.
  leftAt: z.string().optional(),
});
export type ClassStudent = z.infer<typeof classStudentSchema>;

// One row of the roster of a class (F1 #26). The prototype rows also carry the F4 figures; the real
// roster has none (its `status` is always null until F4), so `metrics` is absent there.
export type ClassRosterRow = {
  id: string;
  classId: string;
  name: string;
  email?: string;
  phone?: string;
  joinedAt?: string;
  metrics?: {
    status: StudentStatus;
    avgScore: number | null;
    completionPct: number;
    lastActiveLabel: string;
  };
};

export const inviteCodeSchema = z.object({
  id: z.string(),
  code: z.string(),
  expiresAtLabel: z.string(),
});
export type InviteCode = z.infer<typeof inviteCodeSchema>;

// classProgress.attention.* (Khu vực C — "Cần chú ý").
export const attentionItemSchema = z.object({
  studentId: z.string(),
  classId: z.string(),
  name: z.string(),
  reason: z.string(),
  status: studentStatusSchema,
});
export type AttentionItem = z.infer<typeof attentionItemSchema>;

export const classActivitySchema = z.object({
  id: z.string(),
  text: z.string(),
  timeLabel: z.string(),
  attention: z.boolean(),
});
export type ClassActivity = z.infer<typeof classActivitySchema>;

export const classFormSchema = z.object({
  name: z.string().min(1),
  description: z.string().optional(),
  schedule: classScheduleSchema.optional(),
});
export type ClassForm = z.infer<typeof classFormSchema>;

export const classScoreTrendSchema = z.object({
  weekLabels: z.array(z.string()),
  series: z.array(
    z.object({
      classId: z.string(),
      className: z.string(),
      points: z.array(z.number()),
    }),
  ),
});
export type ClassScoreTrend = z.infer<typeof classScoreTrendSchema>;

// Two-letter avatar initial, same rule for every screen in the cluster
// (class_management Sheet 5 Khu vực D NO 4: "hai ký tự đầu của hai từ cuối trong display_name").
export function initialsOfClassStudent(name: string): string {
  const parts = name.trim().split(/\s+/);
  const lastTwo = parts.slice(-2);
  return lastTwo.map((part) => part.charAt(0).toUpperCase()).join("");
}

// Learner side (screen "Lớp của tôi") - one class the signed-in learner belongs to. Instructor name
// and assigned problems ride along so the screen needs a single call.
export type MyClassAssignment = {
  problemId: string;
  title: string;
  solved: boolean;
  assignedAtLabel: string;
};
export type MyClass = {
  id: string;
  name: string;
  description?: string;
  schedule?: ClassSchedule;
  instructorName: string;
  joinedAtLabel: string;
  // Assigned problems come from F2 (not built): the real class has none, and a screen hides the
  // block and the progress bar rather than show "0 / 0".
  assignments?: MyClassAssignment[];
};

/** What joining by invite code tells the learner (F1 #25). */
export type JoinedClass = { id: string; name: string };
