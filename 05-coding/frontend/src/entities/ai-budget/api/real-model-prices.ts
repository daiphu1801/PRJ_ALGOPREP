// Real calls behind the ADM0303 price table: 03-dd/api/F5_ai-review.md #23 to #26. The record is the
// wire: ModelPriceController (`Item`, `Items`). Paths are relative to /api/v1. Errors (AIR-121, AIR-204,
// AIR-301, AIR-V17, AIR-V18) are rethrown as `ApiError` and shown through `useApiErrorMessage`.
import { api } from "@/shared/api";
import type { ModelPrice } from "../model/types";

const BASE = "/ai/admin/model-prices";

type ItemDto = {
  id: string;
  modelName: string;
  usdPerMillionTokens: number | string;
};

function toPrice(dto: ItemDto): ModelPrice {
  return {
    key: dto.id,
    label: dto.modelName,
    usdPerMillionTokens: Number(dto.usdPerMillionTokens),
  };
}

/** #23: server order (model name, case-insensitive). */
export async function realListModelPrices(): Promise<ModelPrice[]> {
  const body = await api.get<{ items: ItemDto[] }>(BASE);
  return body.items.map(toPrice);
}

/** #24: the price starts at 0; edit it with #25. */
export async function realCreateModelPrice(
  modelName: string,
): Promise<ModelPrice> {
  return toPrice(await api.post<ItemDto>(BASE, { modelName }));
}

/** #25: send only the field that changed. */
export async function realUpdateModelPrice(
  id: string,
  patch: { modelName?: string; usdPerMillionTokens?: number },
): Promise<ModelPrice> {
  return toPrice(await api.patch<ItemDto>(`${BASE}/${id}`, patch));
}

/** #26: 204, no body; deleting an already deleted row is also 204. */
export async function realDeleteModelPrice(id: string): Promise<void> {
  await api.delete(`${BASE}/${id}`);
}
