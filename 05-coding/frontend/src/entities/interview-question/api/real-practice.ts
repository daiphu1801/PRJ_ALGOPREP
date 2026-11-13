// Real calls of practice mode (03-dd/api/F6_interview-bank.md #6 and #7), wire per
// `PracticeAnswersController`. The learner's answer is sent as data in the body and nowhere else.
// #6 error codes: INB-V06, INB-V07, INB-010, INB-040, INB-041, INB-042, INB-050 (all rethrown); a
// failed or invalid AI answer is NOT an error: the call still answers 201 with `feedbackStatus =
// FAILED` and `feedbackErrorCode = INB-501 | INB-502`, which the mapper keeps on the attempt.
// #7 error codes: INB-V08, INB-010.
import { api } from "@/shared/api";
import type {
  PracticeAttempt,
  PracticeAttemptPage,
} from "../model/learner-types";

const path = (questionId: string) =>
  `/interview-bank/questions/${questionId}/practice-answers`;

type AttemptDto = {
  id: string;
  attemptNo: number;
  createdAt: string;
  answerText: string;
  feedbackStatus: "COMPLETED" | "FAILED";
  feedback: {
    achievedPoints: string[];
    missingPoints: string[];
    improvementSuggestions: string[];
    criteria: {
      criterionCode: string;
      achievementPercent: number;
      weightPercent: number;
    }[];
  } | null;
  feedbackErrorCode: string | null;
};

export function toAttempt(dto: AttemptDto): PracticeAttempt {
  const base: PracticeAttempt = {
    attemptNo: dto.attemptNo,
    createdAt: dto.createdAt,
    feedbackStatus: dto.feedbackStatus === "COMPLETED" ? "completed" : "failed",
    answerText: dto.answerText,
    feedbackErrorCode: dto.feedbackErrorCode,
  };
  if (!dto.feedback) return base;
  return {
    ...base,
    strengths: dto.feedback.achievedPoints,
    gaps: dto.feedback.missingPoints,
    // The wire sends a list of suggestions; the screen shows them as lines of one block.
    nextSteps: dto.feedback.improvementSuggestions.join("\n"),
    criteria: dto.feedback.criteria,
  };
}

/** #6: not idempotent, every call is a new attempt; the screen never resends on its own. */
export async function realSubmitPracticeAnswer(
  questionId: string,
  answerText: string,
): Promise<PracticeAttempt> {
  return toAttempt(await api.post<AttemptDto>(path(questionId), { answerText }));
}

/** #7: newest first, 10 per page; `before` is the smallest `attemptNo` already loaded. */
export async function realListAttempts(
  questionId: string,
  before?: number,
): Promise<PracticeAttemptPage> {
  const body = await api.get<{
    items: AttemptDto[];
    hasMore: boolean;
    totalAttempts: number;
  }>(path(questionId), before === undefined ? undefined : { before });
  return {
    items: body.items.map(toAttempt),
    hasMore: body.hasMore,
    totalAttempts: body.totalAttempts,
  };
}
