import { api } from "@/shared/api";
import type {
  InterviewSummary,
  ProgressRange,
  RubricCriterionCode,
} from "../model/types";

type InterviewSummaryDto = {
  completedSessionCount: number;
  averageScore: number | null;
  weakestCriterionCode: RubricCriterionCode | null;
  recentSessions: {
    sessionId: string;
    entryType: string;
    startedAt: string;
    endedAt: string;
    overallScore: number;
    rubricScores: Record<RubricCriterionCode, number>;
  }[];
};

/** F5 #13 is a PostgreSQL read; the server owns range filtering, completed-only counts and weighted scores. */
export async function realGetInterviewSummary(
  range: ProgressRange = "all",
): Promise<InterviewSummary> {
  const dto = await api.get<InterviewSummaryDto>(
    "/ai/me/interview-sessions/summary",
    { range, limit: 3 },
  );
  return {
    completedSessionCount: dto.completedSessionCount,
    averageScore: dto.averageScore,
    weakestCriterionCode: dto.weakestCriterionCode,
    recentSessions: dto.recentSessions.map((session) => ({
      sessionId: session.sessionId,
      startedAt: session.startedAt,
      score: session.overallScore,
      rubricScores: session.rubricScores,
    })),
  };
}
