import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realActiveUsersSummary,
  realPasswordResetsSummary,
  realUserRetention,
} from "./real-dashboard";

const summary = {
  value: 42,
  deltaPercent: 12.5,
  deltaDirection: "up",
  sparklineSeries: [1, 2, 3, 4, 5, 6, 7],
};

describe("real admin overview API", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("reads the active users card from #41 and returns it as sent", async () => {
    get.mockResolvedValue(summary);

    expect(await realActiveUsersSummary()).toEqual(summary);
    expect(get).toHaveBeenCalledWith("/identity/admin/dashboard/summary");
  });

  it("reads the password reset card from #73 and keeps a null delta", async () => {
    const none = { ...summary, deltaPercent: null, deltaDirection: "none" };
    get.mockResolvedValue(none);

    expect(await realPasswordResetsSummary()).toEqual(none);
    expect(get).toHaveBeenCalledWith(
      "/identity/admin/dashboard/password-resets",
    );
  });

  it("maps the six months of #42 to grouped bars labelled T<month>", async () => {
    get.mockResolvedValue({
      groups: [
        { month: "2026-06", newUsers: 3, returningUsers: 0 },
        { month: "2026-11", newUsers: 0, returningUsers: 9 },
      ],
    });

    const result = await realUserRetention();

    expect(get).toHaveBeenCalledWith("/identity/admin/dashboard/user-growth");
    expect(result.groups).toEqual([
      {
        label: "T6",
        bars: [
          { label: "Mới", value: 3, colorVar: "--color-admin-teal" },
          { label: "Quay lại", value: 0, colorVar: "--color-admin-cyan" },
        ],
      },
      {
        label: "T11",
        bars: [
          { label: "Mới", value: 0, colorVar: "--color-admin-teal" },
          { label: "Quay lại", value: 9, colorVar: "--color-admin-cyan" },
        ],
      },
    ]);
    expect(result.legend.map((l) => l.label)).toEqual([
      "Người dùng mới",
      "Quay lại",
    ]);
  });

  it("lets a gateway error propagate so the block shows its own retry", async () => {
    get.mockRejectedValue(new Error("boom"));

    await expect(realUserRetention()).rejects.toThrow("boom");
  });
});
