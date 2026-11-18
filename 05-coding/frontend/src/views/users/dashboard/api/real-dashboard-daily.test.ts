import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

import { ApiError } from "@/shared/api";
import { realGetDashboardDaily } from "./real-dashboard-daily";

beforeEach(() => {
  get.mockReset();
});

const days = (n: number) =>
  Array.from({ length: n }, (_, i) => ({
    day: `2026-11-${String(i + 1).padStart(2, "0")}`,
    count: i,
  }));

describe("realGetDashboardDaily", () => {
  it.each([
    ["7d", 7],
    ["30d", 14],
    ["all", 5],
  ] as const)(
    "range %s: one point per column the server sends (%i)",
    async (range, n) => {
      get.mockResolvedValue({
        streakDays: 0,
        lastSubmittedAt: null,
        dailyCounts: days(n),
      });

      const points = await realGetDashboardDaily(range);

      expect(get).toHaveBeenCalledWith("/judge/me/metrics", { range });
      expect(points).toHaveLength(n);
    },
  );

  it("words the label as dd/MM and keeps the count, zero days included", async () => {
    get.mockResolvedValue({
      streakDays: 1,
      lastSubmittedAt: null,
      dailyCounts: [
        { day: "2026-11-03", count: 0 },
        { day: "2026-11-04", count: 6 },
      ],
    });

    expect(await realGetDashboardDaily("7d")).toEqual([
      { label: "03/11", count: 0 },
      { label: "04/11", count: 6 },
    ]);
  });

  it("rethrows an F4 error so the block shows its error state", async () => {
    get.mockRejectedValue(new ApiError("JDG-507", 503, "down"));

    await expect(realGetDashboardDaily("7d")).rejects.toMatchObject({
      code: "JDG-507",
    });
  });
});
