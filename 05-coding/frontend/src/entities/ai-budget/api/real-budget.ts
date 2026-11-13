// Real calls behind ADM0302 (budget bar) and ADM0303 (cap, lock, limits): 03-dd/api/F5_ai-review.md #18 to #22.
// The record is the wire: TokenBudgetController (`Budget`, `Limits`). Paths are relative to /api/v1.
// Every error is rethrown as `ApiError` (AIR-203, AIR-301, AIR-503, AIR-V15, AIR-V16) and shown by the
// caller through `useApiErrorMessage`. `periodStart` and `lockedAt` are sent but no screen reads them.
import { api } from "@/shared/api";
import type {
  AiFeature,
  RateLimits,
  RateLimitsPatch,
  TokenBudget,
} from "../model/types";

const BASE = "/ai/admin";

type BudgetDto = {
  limitTokens: number;
  usedTokens: number;
  isLocked: boolean;
  projectedRunOutDate: string | null;
  featureBreakdown: { featureCode: string; tokens: number }[];
  warnPercent: number;
};

const FEATURE: Record<string, AiFeature> = {
  SOLUTION_REVIEW: "review",
  MOCK_INTERVIEW: "interview",
  GENERATE_TESTCASE: "testcase",
  PRACTICE_FEEDBACK: "practice",
};

export function toTokenBudget(dto: BudgetDto): TokenBudget {
  const featureTokens: Record<AiFeature, number> = {
    review: 0,
    interview: 0,
    testcase: 0,
    practice: 0,
  };
  for (const row of dto.featureBreakdown) {
    const feature = FEATURE[row.featureCode];
    if (feature) featureTokens[feature] = row.tokens;
  }
  return {
    limitTokens: dto.limitTokens,
    usedTokens: dto.usedTokens,
    locked: dto.isLocked,
    warnPercent: dto.warnPercent,
    projectedRunOutDate: dto.projectedRunOutDate,
    featureTokens,
  };
}

/** #18. */
export async function realGetTokenBudget(): Promise<TokenBudget> {
  return toTokenBudget(await api.get<BudgetDto>(`${BASE}/token-budget`));
}

/** #19: `{ limitTokens }` only. Raising the cap does not unlock. */
export async function realSetTokenLimit(
  limitTokens: number,
): Promise<TokenBudget> {
  return toTokenBudget(
    await api.put<BudgetDto>(`${BASE}/token-budget/limit`, { limitTokens }),
  );
}

/** #20: no body; AIR-203 while usage has not dropped below the cap. */
export async function realUnlockTokenBudget(): Promise<TokenBudget> {
  return toTokenBudget(
    await api.post<BudgetDto>(`${BASE}/token-budget/unlock`),
  );
}

/** #21. */
export async function realGetLimitsConfig(): Promise<RateLimits> {
  return api.get<RateLimits>(`${BASE}/limits-config`).then(toLimits);
}

/** #22: only the fields in `patch` are sent. */
export async function realPatchLimitsConfig(
  patch: RateLimitsPatch,
): Promise<RateLimits> {
  return api.patch<RateLimits>(`${BASE}/limits-config`, patch).then(toLimits);
}

function toLimits(dto: RateLimits): RateLimits {
  return {
    reviewPerHour: dto.reviewPerHour,
    interviewPerHour: dto.interviewPerHour,
    cooldownSeconds: dto.cooldownSeconds,
    warnPercent: dto.warnPercent,
  };
}
