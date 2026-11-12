import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const put = vi.fn();
const post = vi.fn();
const patch = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    put: (...args: unknown[]) => put(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realGetLimitsConfig,
  realGetTokenBudget,
  realPatchLimitsConfig,
  realSetTokenLimit,
  realUnlockTokenBudget,
} from "./real-budget";

const budgetDto = {
  limitTokens: 52_000_000,
  usedTokens: 38_400_000,
  periodStart: "2026-11-01",
  lockedAt: null,
  isLocked: false,
  projectedRunOutDate: "2026-11-28",
  featureBreakdown: [
    { featureCode: "SOLUTION_REVIEW", tokens: 20_900_000 },
    { featureCode: "MOCK_INTERVIEW", tokens: 11_800_000 },
    { featureCode: "GENERATE_TESTCASE", tokens: 3_700_000 },
    { featureCode: "PRACTICE_FEEDBACK", tokens: 2_000_000 },
  ],
  warnPercent: 70,
};

const budgetModel = {
  limitTokens: 52_000_000,
  usedTokens: 38_400_000,
  locked: false,
  warnPercent: 70,
  projectedRunOutDate: "2026-11-28",
  featureTokens: {
    review: 20_900_000,
    interview: 11_800_000,
    testcase: 3_700_000,
    practice: 2_000_000,
  },
};

const limits = {
  reviewPerHour: 10,
  interviewPerHour: 3,
  cooldownSeconds: 20,
  warnPercent: 70,
};

const BASE = "/ai/admin";

beforeEach(() => {
  get.mockReset();
  put.mockReset();
  post.mockReset();
  patch.mockReset();
});

describe("realGetTokenBudget (F5 #18)", () => {
  it("reads the budget and maps it without leaking unread fields", async () => {
    get.mockResolvedValue(budgetDto);
    await expect(realGetTokenBudget()).resolves.toEqual(budgetModel);
    expect(get).toHaveBeenCalledWith(`${BASE}/token-budget`);
  });

  it("fills every feature with 0 when the server omits one, and ignores an unknown code", async () => {
    get.mockResolvedValue({
      ...budgetDto,
      projectedRunOutDate: null,
      featureBreakdown: [
        { featureCode: "SOLUTION_REVIEW", tokens: 5 },
        { featureCode: "SOMETHING_NEW", tokens: 9 },
      ],
    });
    const result = await realGetTokenBudget();
    expect(result.featureTokens).toEqual({
      review: 5,
      interview: 0,
      testcase: 0,
      practice: 0,
    });
    expect(result.projectedRunOutDate).toBeNull();
  });

  it("maps isLocked to locked", async () => {
    get.mockResolvedValue({ ...budgetDto, isLocked: true });
    expect((await realGetTokenBudget()).locked).toBe(true);
  });

  it("rethrows AIR-301", async () => {
    get.mockRejectedValue(new ApiError("AIR-301", 403));
    await expect(realGetTokenBudget()).rejects.toMatchObject({
      code: "AIR-301",
    });
  });
});

describe("realSetTokenLimit (F5 #19)", () => {
  it("PUTs exactly { limitTokens } and maps the answer", async () => {
    put.mockResolvedValue({ ...budgetDto, limitTokens: 60_000_000 });
    const result = await realSetTokenLimit(60_000_000);
    expect(put).toHaveBeenCalledWith(`${BASE}/token-budget/limit`, {
      limitTokens: 60_000_000,
    });
    expect(result.limitTokens).toBe(60_000_000);
  });

  it.each([
    ["AIR-V15", 400],
    ["AIR-301", 403],
    ["AIR-503", 503],
  ])("rethrows %s", async (code, status) => {
    put.mockRejectedValue(new ApiError(code, status));
    await expect(realSetTokenLimit(1)).rejects.toMatchObject({ code, status });
  });
});

describe("realUnlockTokenBudget (F5 #20)", () => {
  it("POSTs without a body and maps the unlocked budget", async () => {
    post.mockResolvedValue(budgetDto);
    const result = await realUnlockTokenBudget();
    expect(post).toHaveBeenCalledWith(`${BASE}/token-budget/unlock`);
    expect(result.locked).toBe(false);
  });

  it.each([
    ["AIR-203", 422],
    ["AIR-301", 403],
    ["AIR-503", 503],
  ])("rethrows %s", async (code, status) => {
    post.mockRejectedValue(new ApiError(code, status));
    await expect(realUnlockTokenBudget()).rejects.toMatchObject({
      code,
      status,
    });
  });
});

describe("realGetLimitsConfig (F5 #21)", () => {
  it("reads the four values, including the cooldown", async () => {
    get.mockResolvedValue({ ...limits, updatedBy: "x" });
    await expect(realGetLimitsConfig()).resolves.toEqual(limits);
    expect(get).toHaveBeenCalledWith(`${BASE}/limits-config`);
  });

  it("rethrows AIR-301", async () => {
    get.mockRejectedValue(new ApiError("AIR-301", 403));
    await expect(realGetLimitsConfig()).rejects.toMatchObject({
      code: "AIR-301",
    });
  });
});

describe("realPatchLimitsConfig (F5 #22)", () => {
  it("PATCHes only the field that changed", async () => {
    patch.mockResolvedValue({ ...limits, reviewPerHour: 5 });
    const result = await realPatchLimitsConfig({ reviewPerHour: 5 });
    expect(patch).toHaveBeenCalledWith(`${BASE}/limits-config`, {
      reviewPerHour: 5,
    });
    expect(result.reviewPerHour).toBe(5);
  });

  it("sends warnPercent and cooldownSeconds the same way", async () => {
    patch.mockResolvedValue(limits);
    await realPatchLimitsConfig({ warnPercent: 80 });
    await realPatchLimitsConfig({ cooldownSeconds: 30 });
    expect(patch).toHaveBeenNthCalledWith(1, `${BASE}/limits-config`, {
      warnPercent: 80,
    });
    expect(patch).toHaveBeenNthCalledWith(2, `${BASE}/limits-config`, {
      cooldownSeconds: 30,
    });
  });

  it.each([
    ["AIR-V16", 400],
    ["AIR-301", 403],
  ])("rethrows %s", async (code, status) => {
    patch.mockRejectedValue(new ApiError(code, status));
    await expect(
      realPatchLimitsConfig({ reviewPerHour: 1 }),
    ).rejects.toMatchObject({ code, status });
  });
});
