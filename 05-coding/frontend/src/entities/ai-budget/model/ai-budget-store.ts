// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// ADMIN-set AI budget parameters (ADM0302 Q2 and Q7, DEC-2026-1001-admin-configurable-settings): the
// warning threshold of the budget bar and the token price per model. They are edited on the AI
// config screen and read by the usage screen, hence an entity both views import.
"use client";

import {
  createManagedListStore,
  type ManagedItem,
} from "@/shared/lib/managed-list-store";
import { createSettingsStore } from "@/shared/lib/settings-store";

/** The bar changes colour once usage reaches this percent of the cap. 70 is the RD's own example. */
export const aiBudgetSettings = createSettingsStore({ warnPercent: 70 });

export type ModelPrice = ManagedItem & {
  /** USD per one million tokens. */
  usdPerMillionTokens: number;
};

const prices = createManagedListStore<ModelPrice>(
  [{ key: "model-default", label: "Mô hình mặc định", usdPerMillionTokens: 3 }],
  "model",
);

export const useModelPrices = prices.use;
export const addModelPrice = (label: string) =>
  prices.add(label, { usdPerMillionTokens: 0 });
export const renameModelPrice = (key: string, label: string) =>
  prices.update(key, { label });
export const setModelPriceValue = (key: string, usdPerMillionTokens: number) =>
  prices.update(key, { usdPerMillionTokens });
export const removeModelPrice = prices.remove;
