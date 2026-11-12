// Mock-mode behaviour of F5 #18 to #26, backed by the in-memory stores. Each function answers with the
// shape the real call maps to, and throws the same `ApiError` code the server would, so the hooks and
// the screens handle both modes through one path.
import { ApiError } from "@/shared/api";
import {
  aiBudgetSettings,
  aiRateLimits,
  aiTokenBudget,
  mockModelPrices,
} from "../model/ai-budget-store";
import type {
  ModelPrice,
  RateLimits,
  RateLimitsPatch,
  TokenBudget,
} from "../model/types";

// Sums to the store's default usedTokens (38.4 million), from 09-layoutBase/Admin - Token AI.dc.html:482-486.
const FEATURE_TOKENS = {
  review: 20_900_000,
  interview: 11_800_000,
  testcase: 3_700_000,
  practice: 2_000_000,
};

export function mockGetTokenBudget(): TokenBudget {
  const { limitTokens, usedTokens, locked } = aiTokenBudget.get();
  return {
    limitTokens,
    usedTokens,
    locked,
    warnPercent: aiBudgetSettings.get().warnPercent,
    projectedRunOutDate: locked ? null : "2026-08-28",
    featureTokens: { ...FEATURE_TOKENS },
  };
}

export function mockSetTokenLimit(limitTokens: number): TokenBudget {
  // The server locks on the next AI call once usage reaches the cap; the mock does it here.
  aiTokenBudget.set({
    limitTokens,
    ...(limitTokens <= aiTokenBudget.get().usedTokens ? { locked: true } : {}),
  });
  return mockGetTokenBudget();
}

export function mockUnlockTokenBudget(): TokenBudget {
  const { limitTokens, usedTokens } = aiTokenBudget.get();
  if (usedTokens >= limitTokens) throw new ApiError("AIR-203", 422);
  aiTokenBudget.set({ locked: false });
  return mockGetTokenBudget();
}

export function mockGetLimitsConfig(): RateLimits {
  return {
    ...aiRateLimits.get(),
    warnPercent: aiBudgetSettings.get().warnPercent,
  };
}

export function mockPatchLimitsConfig(patch: RateLimitsPatch): RateLimits {
  const { warnPercent, ...rate } = patch;
  aiRateLimits.set(rate);
  if (warnPercent !== undefined) aiBudgetSettings.set({ warnPercent });
  return mockGetLimitsConfig();
}

/** True once at least one price row exists; the usage mock shows "-" cost otherwise. */
export const mockHasModelPrices = () => mockModelPrices.get().length > 0;

export const mockListModelPrices = (): ModelPrice[] => [
  ...mockModelPrices.get(),
];

function failure(code: "empty" | "duplicate"): never {
  throw code === "empty"
    ? new ApiError("AIR-V17", 400)
    : new ApiError("AIR-204", 409);
}

export function mockCreateModelPrice(modelName: string): ModelPrice {
  const error = mockModelPrices.add(modelName, { usdPerMillionTokens: 0 });
  if (error) failure(error);
  return mockModelPrices.get().at(-1)!;
}

export function mockUpdateModelPrice(
  id: string,
  patch: { modelName?: string; usdPerMillionTokens?: number },
): ModelPrice {
  const error = mockModelPrices.update(id, {
    ...(patch.modelName === undefined ? {} : { label: patch.modelName }),
    ...(patch.usdPerMillionTokens === undefined
      ? {}
      : { usdPerMillionTokens: patch.usdPerMillionTokens }),
  });
  if (error) failure(error);
  return mockModelPrices.get().find((price) => price.key === id)!;
}

export function mockDeleteModelPrice(id: string): void {
  mockModelPrices.remove(id);
}
