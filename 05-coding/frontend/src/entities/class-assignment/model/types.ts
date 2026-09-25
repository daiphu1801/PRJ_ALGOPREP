// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { z } from "zod";

export const difficultySchema = z.enum(["EASY", "MEDIUM", "HARD"]);
export type Difficulty = z.infer<typeof difficultySchema>;

// classAssignments.assignmentList.* (02-bd/screens/teacher/INS0202_class_assignments.md Sheet 5 Khu vực C).
export const assignedProblemSchema = z.object({
  id: z.string(),
  title: z.string(),
  topic: z.string(),
  difficulty: difficultySchema,
  assignedClassIds: z.array(z.string()),
  assignedClassNames: z.array(z.string()),
  submissionCount: z.number(),
  acRate: z.number(),
});
export type AssignedProblem = z.infer<typeof assignedProblemSchema>;

export const assignmentSummarySchema = z.object({
  assignedCount: z.number(),
  classCount: z.number(),
  weeklySubmissionCount: z.number(),
  weeklySubmissionDelta: z.number(),
  avgAcRate: z.number().nullable(),
  pendingGrading: z.number(),
});
export type AssignmentSummary = z.infer<typeof assignmentSummarySchema>;
