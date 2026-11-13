import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

import { ApiError } from "@/shared/api";
import { fetchMyMetrics } from "./real-metrics";

beforeEach(() => {
  get.mockReset();
});

describe("fetchMyMetrics (F4 /me/metrics)", () => {
  it("sends only the range when no topic is asked for, and no user id", async () => {
    get.mockResolvedValue({ streakDays: 0, dailyCounts: [] });

    await fetchMyMetrics("7d");

    expect(get).toHaveBeenCalledWith("/judge/me/metrics", { range: "7d" });
  });

  it("repeats topicIds when topics are given", async () => {
    get.mockResolvedValue({ streakDays: 0, dailyCounts: [] });

    await fetchMyMetrics("all", ["a", "b"]);

    expect(get).toHaveBeenCalledWith("/judge/me/metrics", {
      range: "all",
      topicIds: ["a", "b"],
    });
  });

  it.each(["JDG-V07", "JDG-V15", "JDG-507"])(
    "rethrows %s untouched",
    async (code) => {
      get.mockRejectedValue(new ApiError(code, 400, "x"));

      await expect(fetchMyMetrics("7d")).rejects.toMatchObject({ code });
    },
  );
});
