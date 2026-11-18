import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const patch = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realCreateModelPrice,
  realDeleteModelPrice,
  realListModelPrices,
  realUpdateModelPrice,
} from "./real-model-prices";

const ID = "11111111-1111-4111-8111-111111111111";
const BASE = "/ai/admin/model-prices";

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  patch.mockReset();
  del.mockReset();
});

describe("realListModelPrices (F5 #23)", () => {
  it("maps id to key, modelName to label and a numeric string to a number", async () => {
    get.mockResolvedValue({
      items: [
        { id: ID, modelName: "gpt-4o", usdPerMillionTokens: 2.5 },
        { id: "b", modelName: "claude", usdPerMillionTokens: "0.1500" },
      ],
    });
    await expect(realListModelPrices()).resolves.toEqual([
      { key: ID, label: "gpt-4o", usdPerMillionTokens: 2.5 },
      { key: "b", label: "claude", usdPerMillionTokens: 0.15 },
    ]);
    expect(get).toHaveBeenCalledWith(BASE);
  });

  it("rethrows AIR-301", async () => {
    get.mockRejectedValue(new ApiError("AIR-301", 403));
    await expect(realListModelPrices()).rejects.toMatchObject({
      code: "AIR-301",
    });
  });
});

describe("realCreateModelPrice (F5 #24)", () => {
  it("POSTs exactly { modelName }", async () => {
    post.mockResolvedValue({
      id: ID,
      modelName: "gpt-4o",
      usdPerMillionTokens: 0,
    });
    const result = await realCreateModelPrice("gpt-4o");
    expect(post).toHaveBeenCalledWith(BASE, { modelName: "gpt-4o" });
    expect(result).toEqual({
      key: ID,
      label: "gpt-4o",
      usdPerMillionTokens: 0,
    });
  });

  it.each([
    ["AIR-V17", 400],
    ["AIR-204", 409],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realCreateModelPrice("x")).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realUpdateModelPrice (F5 #25)", () => {
  it("PATCHes only the price when only the price changed", async () => {
    patch.mockResolvedValue({
      id: ID,
      modelName: "m",
      usdPerMillionTokens: 1.2,
    });
    await realUpdateModelPrice(ID, { usdPerMillionTokens: 1.2 });
    expect(patch).toHaveBeenCalledWith(`${BASE}/${ID}`, {
      usdPerMillionTokens: 1.2,
    });
  });

  it("PATCHes only the name when only the name changed", async () => {
    patch.mockResolvedValue({
      id: ID,
      modelName: "new",
      usdPerMillionTokens: 3,
    });
    const result = await realUpdateModelPrice(ID, { modelName: "new" });
    expect(patch).toHaveBeenCalledWith(`${BASE}/${ID}`, { modelName: "new" });
    expect(result).toEqual({ key: ID, label: "new", usdPerMillionTokens: 3 });
  });

  it.each([
    ["AIR-V17", 400],
    ["AIR-V18", 400],
    ["AIR-121", 404],
    ["AIR-204", 409],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    patch.mockRejectedValue(new ApiError(code, status));
    await expect(
      realUpdateModelPrice(ID, { usdPerMillionTokens: 1 }),
    ).rejects.toMatchObject({ code, status });
  });
});

describe("realDeleteModelPrice (F5 #26)", () => {
  it("DELETEs the row and resolves with nothing", async () => {
    del.mockResolvedValue(undefined);
    await expect(realDeleteModelPrice(ID)).resolves.toBeUndefined();
    expect(del).toHaveBeenCalledWith(`${BASE}/${ID}`);
  });

  it("rethrows AIR-301", async () => {
    del.mockRejectedValue(new ApiError("AIR-301", 403));
    await expect(realDeleteModelPrice(ID)).rejects.toMatchObject({
      code: "AIR-301",
    });
  });
});
