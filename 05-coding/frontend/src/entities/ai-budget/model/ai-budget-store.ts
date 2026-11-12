// MOCK-MODE stores only. In real mode the same data comes from F5 #18 to #26 (api/real-*.ts); the hooks
// in api/queries.ts pick the source, so a screen never reads these directly.
//
// ADMIN-set AI budget parameters (ADM0302 Q2 and Q7, DEC-2026-1001-admin-configurable-settings): the
// warning threshold of the budget bar and the token price per model.
"use client";

import { createManagedListStore } from "@/shared/lib/managed-list-store";
import { createSettingsStore } from "@/shared/lib/settings-store";
import type { ModelPrice } from "./types";

/** The bar changes colour once usage reaches this percent of the cap. 70 is the RD's own example. */
export const aiBudgetSettings = createSettingsStore({ warnPercent: 70 });

export const mockModelPrices = createManagedListStore<ModelPrice>(
  [{ key: "model-default", label: "Mô hình mặc định", usdPerMillionTokens: 3 }],
  "model",
);

/**
 * Per-user AI rate limits, per HOUR (owner 2026-10-10, BD ADM0303 Khu vực C). Defaults follow
 * 02-bd/database/F5_ai-review.md section 6; the 20 s wait comes from the old mockup.
 */
export const aiRateLimits = createSettingsStore({
  reviewPerHour: 10,
  interviewPerHour: 3,
  cooldownSeconds: 20,
});

/**
 * Monthly token cap and its lock (F5-25, BD ADM0303 Khu vực B). The real lock is set by the server
 * when usage passes the cap; the mock sets it when the cap is saved at or below usage.
 */
export const aiTokenBudget = createSettingsStore({
  limitTokens: 52_000_000,
  usedTokens: 38_400_000,
  locked: false,
});
