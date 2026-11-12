import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.hoisted(() => vi.fn());
vi.mock("@/shared/api", async (original) => ({
  ...(await original<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
  withMockData: (_mock: unknown, real: () => unknown) => real(),
}));

import { realGetInterviewSummary } from "./real-interview-summary";
import { useInterviewSummary } from "./queries";

const scored = {
  completedSessionCount: 12,
  averageScore: 7.82,
  weakestCriterionCode: "CLARITY",
  recentSessions: [
    {
      sessionId: "session-mine",
      entryType: "THEORY",
      startedAt: "2026-11-01T01:00:00Z",
      endedAt: "2026-11-01T01:30:00Z",
      overallScore: 6.75,
      rubricScores: {
        CLARITY: 0,
        TECHNICAL_ACCURACY: 9,
        PUSHBACK_HANDLING: 8,
        COMPLEXITY_AWARENESS: 10,
      },
    },
  ],
};

describe("real interview summary F5 #13", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("uses caller-scoped API, asks for three recent sessions and preserves server totals and weighted scores", async () => {
    get.mockResolvedValue(scored);
    expect(await realGetInterviewSummary()).toEqual({
      completedSessionCount: 12,
      averageScore: 7.82,
      weakestCriterionCode: "CLARITY",
      recentSessions: [
        {
          sessionId: "session-mine",
          startedAt: "2026-11-01T01:00:00Z",
          score: 6.75,
          rubricScores: scored.recentSessions[0]!.rubricScores,
        },
      ],
    });
    expect(get).toHaveBeenCalledExactlyOnceWith(
      "/ai/me/interview-sessions/summary",
      { range: "all", limit: 3 },
    );
  });

  it.each(["7d", "30d", "all"] as const)(
    "passes %s to the server and preserves its honest empty result",
    async (range) => {
      const empty = {
        completedSessionCount: 0,
        averageScore: null,
        weakestCriterionCode: null,
        recentSessions: [],
      };
      get.mockResolvedValue(empty);
      expect(await realGetInterviewSummary(range)).toEqual(empty);
      expect(get).toHaveBeenCalledWith("/ai/me/interview-sessions/summary", {
        range,
        limit: 3,
      });
    },
  );

  it.each([
    ["AIR-501", 503],
    ["IDT-401", 401],
  ])(
    "propagates %s instead of inventing an empty summary",
    async (code, status) => {
      const error = new ApiError(String(code), Number(status), "unavailable");
      get.mockRejectedValue(error);
      await expect(realGetInterviewSummary("7d")).rejects.toBe(error);
    },
  );

  it("the existing dashboard/profile hook reaches the real adapter instead of the old501 stub", async () => {
    get.mockResolvedValue(scored);
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    );
    const hook = renderHook(() => useInterviewSummary("30d"), { wrapper });
    await waitFor(() => expect(hook.result.current.isSuccess).toBe(true));
    expect(hook.result.current.data?.completedSessionCount).toBe(12);
    expect(hook.result.current.data?.recentSessions[0]?.score).toBe(6.75);
    expect(get).toHaveBeenCalledExactlyOnceWith(
      "/ai/me/interview-sessions/summary",
      { range: "30d", limit: 3 },
    );
    client.clear();
  });
});
