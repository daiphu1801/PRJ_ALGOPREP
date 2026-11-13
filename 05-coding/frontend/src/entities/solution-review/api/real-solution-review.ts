// Real F5 Solution Review calls: 03-dd/api/F5_ai-review.md sections 4 to 7, controller
// `SolutionReviewController`. The Java records are the wire; the DD is prose.
//
// Dropped on purpose: `id`, `servedFromCache`, `isReferenceOnly` (no screen reads them). The report
// carries no problem title, level, source code or submission model: the view joins those from F4
// `GET /judge/submissions/{id}`, nothing is invented here.
import { api, ApiError } from "@/shared/api";
import {
  AIR_IN_PROGRESS,
  AIR_PROVIDER_UNAVAILABLE,
} from "../model/review-errors";
import type {
  ComplexityAnalysis,
  ReviewVerdict,
  SolutionReviewDetail,
} from "../model/types";

const BASE = "/ai";
/** BR-SC-01 step 3: server waits up to 30 s for the LLM plus two 3 s gate waits, plus margin. */
export const CLIENT_TIMEOUT_MS = 40_000;
/** BR-SC-01 step 4: AIR-103 means another call is generating this report; wait and ask again. */
export const IN_PROGRESS_WAIT_MS = 3_000;
export const IN_PROGRESS_MAX_RETRIES = 10;

type ComplexityWire = {
  actualTime: string | null;
  optimalTime: string | null;
  timeExplanation: string | null;
  actualSpace: string | null;
  optimalSpace: string | null;
  spaceExplanation: string | null;
};

type ReviewWire = {
  submissionId: string;
  problemId: string;
  language: "JAVA" | "CPP" | "PYTHON";
  createdAt: string;
  readabilityScore: number;
  schemaVersion: string;
  approachTitle: string;
  verdict: string;
  isApproachOptimal: boolean;
  complexity: ComplexityWire;
  strengths: string[];
  improvements: string[];
  edgeCases: string[];
  followUpTopics: string[];
  codeDiff: { suggestedCode: string | null; explanation: string | null } | null;
};

const VERDICTS: readonly ReviewVerdict[] = [
  "OPTIMAL",
  "IMPROVABLE",
  "NOT_OPTIMAL",
];

/** An unknown or empty verdict string falls back to what `isApproachOptimal` already says. */
export function toVerdict(value: string, optimal = false): ReviewVerdict {
  return (
    VERDICTS.find((verdict) => verdict === value) ??
    (optimal ? "OPTIMAL" : "IMPROVABLE")
  );
}

function toComplexity(wire: ComplexityWire): ComplexityAnalysis {
  return {
    actualTime: wire.actualTime ?? "—",
    optimalTime: wire.optimalTime,
    timeExplanation: wire.timeExplanation ?? "",
    actualSpace: wire.actualSpace ?? "—",
    optimalSpace: wire.optimalSpace,
    spaceExplanation: wire.spaceExplanation ?? "",
  };
}

export function toSolutionReview(wire: ReviewWire): SolutionReviewDetail {
  return {
    submissionId: wire.submissionId,
    problemId: wire.problemId,
    languageKey: wire.language.toLowerCase() as SolutionReviewDetail["languageKey"],
    readabilityScore: wire.readabilityScore,
    createdAt: wire.createdAt,
    schemaVersion: wire.schemaVersion,
    approachTitle: wire.approachTitle,
    verdict: toVerdict(wire.verdict, wire.isApproachOptimal),
    isApproachOptimal: wire.isApproachOptimal,
    complexity: toComplexity(wire.complexity),
    strengths: wire.strengths,
    improvements: wire.improvements,
    edgeCases: wire.edgeCases,
    followUpTopics: wire.followUpTopics,
    codeDiff: {
      suggestedCode: wire.codeDiff?.suggestedCode ?? null,
      explanation: wire.codeDiff?.explanation ?? null,
    },
  };
}

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(signal.reason);
      return;
    }
    const timer = setTimeout(resolve, ms);
    signal?.addEventListener(
      "abort",
      () => {
        clearTimeout(timer);
        reject(signal.reason);
      },
      { once: true },
    );
  });
}

/**
 * #4 `POST /ai/submissions/{id}/solution-review`: idempotent get-or-create, no body (the server reads
 * the code itself, so user code never travels through this call). Each attempt has its own 40 s
 * limit; a timeout is a transient AIR-501. AIR-103 is waited out inside this function so the query
 * stays `isFetching` and the skeleton does not flicker.
 */
export async function getRealSolutionReview(
  submissionId: string,
  signal?: AbortSignal,
): Promise<SolutionReviewDetail> {
  for (let attempt = 0; ; attempt++) {
    const timeout = AbortSignal.timeout(CLIENT_TIMEOUT_MS);
    const attemptSignal = signal ? AbortSignal.any([signal, timeout]) : timeout;
    try {
      const wire = await api.post<ReviewWire>(
        `${BASE}/submissions/${encodeURIComponent(submissionId)}/solution-review`,
        undefined,
        { signal: attemptSignal },
      );
      return toSolutionReview(wire);
    } catch (error) {
      if (signal?.aborted) throw error;
      if (timeout.aborted) {
        throw new ApiError(AIR_PROVIDER_UNAVAILABLE, 503, "client timeout");
      }
      if (!(error instanceof ApiError) || error.code !== AIR_IN_PROGRESS) {
        throw error;
      }
      if (attempt >= IN_PROGRESS_MAX_RETRIES - 1) {
        throw new ApiError(AIR_PROVIDER_UNAVAILABLE, 503, "report still busy");
      }
      await sleep(IN_PROGRESS_WAIT_MS, signal);
    }
  }
}

/** #5 `GET /ai/me/solution-reviews/problem-ids`: problems the caller has a stored review for. */
export async function getRealReviewedProblemIds(): Promise<string[]> {
  const body = await api.get<{ problemIds: string[] }>(
    `${BASE}/me/solution-reviews/problem-ids`,
  );
  return body.problemIds;
}

export type ReviewQuickInsight = {
  submissionId: string;
  timeComplexity: string;
  spaceComplexity: string;
  verdict: ReviewVerdict;
};

/** #6 `GET /ai/me/solution-reviews/quick-insight?problemId=`: `review` is null when none exists. */
export async function getRealReviewQuickInsight(
  problemId: string,
): Promise<ReviewQuickInsight | null> {
  const body = await api.get<{
    review: {
      submissionId: string;
      timeComplexity: string;
      spaceComplexity: string;
      verdict: string;
    } | null;
  }>(`${BASE}/me/solution-reviews/quick-insight`, { problemId });
  const review = body.review;
  if (!review) return null;
  return {
    submissionId: review.submissionId,
    timeComplexity: review.timeComplexity,
    spaceComplexity: review.spaceComplexity,
    verdict: toVerdict(review.verdict),
  };
}

export type RecentReviewItem = {
  submissionId: string;
  problemTitle: string;
  verdict: ReviewVerdict;
  timeComplexity: string;
  createdAt: string;
};

/** #7 `GET /ai/me/solution-reviews/recent?limit=` (1 to 10, server default 3). */
export async function getRealRecentReviews(
  limit?: number,
): Promise<RecentReviewItem[]> {
  const body = await api.get<{
    items: {
      submissionId: string;
      problemTitle: string;
      verdict: string;
      timeComplexity: string;
      createdAt: string;
    }[];
  }>(`${BASE}/me/solution-reviews/recent`, limit ? { limit } : undefined);
  return body.items.map((item) => ({
    submissionId: item.submissionId,
    problemTitle: item.problemTitle,
    verdict: toVerdict(item.verdict),
    timeComplexity: item.timeComplexity,
    createdAt: item.createdAt,
  }));
}
