// Server-backed hooks of the AI budget slice (F5 #18 to #26). In mock mode the same hooks run on the
// in-memory stores (api/mock-budget.ts), so the screens do not branch on the mode. Pessimistic writes:
// the cache is set from the server's answer, never from the value the admin typed.
"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError, useMockQuery, withMockData } from "@/shared/api";
import { useApiErrorMessage } from "@/shared/i18n";
import type { ManagedListResult } from "@/shared/ui";
import { toast } from "@/shared/lib/toast-store";
import type { RateLimitsPatch } from "../model/types";
import {
  mockCreateModelPrice,
  mockDeleteModelPrice,
  mockGetLimitsConfig,
  mockGetTokenBudget,
  mockListModelPrices,
  mockPatchLimitsConfig,
  mockSetTokenLimit,
  mockUnlockTokenBudget,
  mockUpdateModelPrice,
} from "./mock-budget";
import {
  realGetLimitsConfig,
  realGetTokenBudget,
  realPatchLimitsConfig,
  realSetTokenLimit,
  realUnlockTokenBudget,
} from "./real-budget";
import {
  realCreateModelPrice,
  realDeleteModelPrice,
  realListModelPrices,
  realUpdateModelPrice,
} from "./real-model-prices";

const SPEC = "03-dd/api/F5_ai-review.md";
export const BUDGET_KEY = ["ai-budget", "status"] as const;
export const LIMITS_KEY = ["ai-budget", "limits"] as const;
export const PRICES_KEY = ["ai-budget", "model-prices"] as const;

/** #18. `staleTime` 0: a reopened screen shows the lock and the cap as the server has them now. */
export const useTokenBudget = () =>
  useMockQuery({
    queryKey: BUDGET_KEY,
    mock: mockGetTokenBudget,
    real: realGetTokenBudget,
    spec: SPEC,
    staleTime: 0,
  });

/** #19. */
export function useSetTokenLimit() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (limitTokens: number) =>
      withMockData(
        () => mockSetTokenLimit(limitTokens),
        () => realSetTokenLimit(limitTokens),
      ),
    onSuccess: (budget) => client.setQueryData(BUDGET_KEY, budget),
  });
}

/** #20. */
export function useUnlockTokenBudget() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () =>
      withMockData(mockUnlockTokenBudget, realUnlockTokenBudget),
    onSuccess: (budget) => client.setQueryData(BUDGET_KEY, budget),
  });
}

/** #21. */
export const useLimitsConfig = () =>
  useMockQuery({
    queryKey: LIMITS_KEY,
    mock: mockGetLimitsConfig,
    real: realGetLimitsConfig,
    spec: SPEC,
    staleTime: 0,
  });

/** #22: the budget bar reads `warnPercent` too, so its cache is refreshed after a write. */
export function useUpdateLimitsConfig() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (patch: RateLimitsPatch) =>
      withMockData(
        () => mockPatchLimitsConfig(patch),
        () => realPatchLimitsConfig(patch),
      ),
    onSuccess: (limits) => {
      client.setQueryData(LIMITS_KEY, limits);
      return client.invalidateQueries({ queryKey: BUDGET_KEY });
    },
  });
}

/** #23. */
export const useModelPrices = () =>
  useMockQuery({
    queryKey: PRICES_KEY,
    mock: mockListModelPrices,
    real: realListModelPrices,
    spec: SPEC,
    staleTime: 0,
  });

/**
 * #24 to #26 in the shape `ManagedListDialog` calls. A failure is shown here as a toast through
 * `useApiErrorMessage` and answered `"reported"`; an empty name never reaches the server.
 */
export function useModelPriceActions() {
  const client = useQueryClient();
  const toMessage = useApiErrorMessage();
  const refresh = () => client.invalidateQueries({ queryKey: PRICES_KEY });

  async function run(call: () => Promise<unknown>): Promise<ManagedListResult> {
    try {
      await call();
      await refresh();
      return null;
    } catch (error) {
      toast.error(toMessage(error));
      // The row was deleted elsewhere: drop it from the list.
      if (error instanceof ApiError && error.code === "AIR-121") void refresh();
      return "reported";
    }
  }

  return {
    add: (label: string): Promise<ManagedListResult> | ManagedListResult =>
      label.trim()
        ? run(() =>
            withMockData(
              () => mockCreateModelPrice(label),
              () => realCreateModelPrice(label.trim()),
            ),
          )
        : "empty",
    rename: (
      key: string,
      label: string,
    ): Promise<ManagedListResult> | ManagedListResult =>
      label.trim()
        ? run(() =>
            withMockData(
              () => mockUpdateModelPrice(key, { modelName: label }),
              () => realUpdateModelPrice(key, { modelName: label.trim() }),
            ),
          )
        : "empty",
    setPrice: (key: string, usdPerMillionTokens: number) =>
      run(() =>
        withMockData(
          () => mockUpdateModelPrice(key, { usdPerMillionTokens }),
          () => realUpdateModelPrice(key, { usdPerMillionTokens }),
        ),
      ),
    remove: (key: string) =>
      run(() =>
        withMockData(
          () => mockDeleteModelPrice(key),
          () => realDeleteModelPrice(key),
        ),
      ),
  };
}
