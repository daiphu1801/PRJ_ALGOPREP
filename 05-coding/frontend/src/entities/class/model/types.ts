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
  completionPct: z.number(),
  avgScore: z.number().nullable(),
  absentCount: z.number(),
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
