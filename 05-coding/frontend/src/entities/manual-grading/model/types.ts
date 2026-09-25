// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Manual grading queue for instructors — 02-bd/screens/teacher/INS0301_grading.md. Named
// "manual-grading" (not "solution-review") to keep it distinct from entities/solution-review, which
// is the STUDENT-facing one-shot AI report (USR0301) — same underlying `ai.solution_reviews` table,
// two different frontend shapes. Also backs instructor_overview's "Cần chấm tay" widget (top-5 slice
// of the same queue).
import { z } from "zod";

export const manualGradingItemSchema = z.object({
  id: z.string(),
  studentId: z.string(),
  studentName: z.string(),
  problemTitle: z.string(),
  classId: z.string(),
  className: z.string(),
  submittedAtLabel: z.string(),
  createdAt: z.string(),
  aiScore10: z.number(),
  manualScore: z.number().nullable(),
  manualComment: z.string().nullable(),
});
export type ManualGradingItem = z.infer<typeof manualGradingItemSchema>;

export const manualGradingStatsSchema = z.object({
  pendingCount: z.number(),
  gradedCount7d: z.number(),
  avgManualScore: z.number().nullable(),
});
export type ManualGradingStats = z.infer<typeof manualGradingStatsSchema>;

export type GradingStatusFilter = "pending" | "graded" | "all";
