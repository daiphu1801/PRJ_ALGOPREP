// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { z } from "zod";

// A level key from the admin-managed problem levels (entities/problem/model/level-store), not a fixed enum.
export const difficultySchema = z.string().min(1);
export type Difficulty = z.infer<typeof difficultySchema>;

// classAssignments.assignmentList.* (02-bd/screens/teacher/INS0202_class_assignments.md Sheet 5 Khu vực C).
export const assignedProblemSchema = z.object({
  id: z.string(),
  title: z.string(),
  topic: z.string(),
  difficulty: difficultySchema,
  assignedClassIds: z.array(z.string()),
  assignedClassNames: z.array(z.string()),
  /** Every topic of the problem (N-N); the list shows the first one alphabetically plus "+n". */
  topics: z.array(z.string()).optional(),
  /** Whole-system counters of the problem, or the sum over the instructor's classes. */
  submissionCount: z.number(),
  acRate: z.number(),
  /**
   * The same two figures inside each class of the instructor (INS0202 Q11, DEC-2026-1014-instructor-batch2-weekly-rules):
   * the list shows them when a class tab is selected; learners who left the class do not count.
   */
  classStats: z
    .record(
      z.string(),
      z.object({ submissionCount: z.number(), acRate: z.number() }),
    )
    .optional(),
  // Unpublished after it was assigned (INS0202 Q10): stays listed, flagged, cannot be assigned again.
  withdrawn: z.boolean().optional(),
});
export type AssignedProblem = z.infer<typeof assignedProblemSchema>;

export const assignmentSummarySchema = z.object({
  assignedCount: z.number(),
  classCount: z.number(),
});
export type AssignmentSummary = z.infer<typeof assignmentSummarySchema>;

// A published bank problem that can still be given to a class (02-bd/screens/teacher/INS0202_class_assignments.md).
export type AssignableProblem = Pick<
  AssignedProblem,
  "id" | "title" | "topic" | "difficulty"
>;
