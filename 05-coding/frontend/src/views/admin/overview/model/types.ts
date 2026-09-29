// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Zod DTOs for the 9 admin_overview data blocks (02-bd/screens/admin/admin_overview.md section 1).
// One schema per block so a future 03-dd/api/identity.md response can `.parse()` straight into
// these types (Anti-Corruption Layer, nextjs-fsd-expert Layer 3).
import { z } from "zod";

export const statSummarySchema = z.object({
  value: z.number(),
  deltaPercent: z.number(),
  deltaDirection: z.enum(["up", "down"]),
  sparklineSeries: z.array(z.number()),
});
export type StatSummary = z.infer<typeof statSummarySchema>;

export const submissionsByLanguageSchema = z.object({
  pointLabels: z.array(z.string()),
  series: z.array(z.object({ label: z.string(), colorVar: z.string(), points: z.array(z.number()) })),
});
export type SubmissionsByLanguage = z.infer<typeof submissionsByLanguageSchema>;

export const verdictDistributionSchema = z.object({
  slices: z.array(z.object({ label: z.string(), value: z.number(), colorVar: z.string() })),
});
export type VerdictDistribution = z.infer<typeof verdictDistributionSchema>;

export const difficultyBreakdownSchema = z.object({
  legend: z.array(z.object({ label: z.string(), value: z.number(), colorVar: z.string() })),
  groups: z.array(
    z.object({
      label: z.string(),
      bars: z.array(z.object({ label: z.string(), value: z.number(), colorVar: z.string() })),
    }),
  ),
});
export type DifficultyBreakdown = z.infer<typeof difficultyBreakdownSchema>;

export const submissionsByDaySchema = z.object({
  columns: z.array(z.object({ label: z.string(), intensity: z.number() })),
  weekTotal: z.number(),
  dailyAverage: z.number(),
});
export type SubmissionsByDay = z.infer<typeof submissionsByDaySchema>;

export const submissionsByMonthSchema = z.object({
  points: z.array(z.object({ label: z.string(), value: z.number() })),
  monthTotal: z.number(),
});
export type SubmissionsByMonth = z.infer<typeof submissionsByMonthSchema>;

export const topProblemsSchema = z.object({
  items: z.array(z.object({ label: z.string(), value: z.number() })),
});
export type TopProblems = z.infer<typeof topProblemsSchema>;

export const userRetentionSchema = z.object({
  legend: z.array(z.object({ label: z.string(), value: z.number(), colorVar: z.string() })),
  groups: z.array(
    z.object({
      label: z.string(),
      bars: z.array(z.object({ label: z.string(), value: z.number(), colorVar: z.string() })),
    }),
  ),
});
export type UserRetention = z.infer<typeof userRetentionSchema>;
