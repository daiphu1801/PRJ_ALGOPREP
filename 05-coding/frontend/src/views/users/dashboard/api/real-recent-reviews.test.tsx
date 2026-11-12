// Dashboard "recent reviews" block in real mode (F5 #7): mapping to the block model, and failing alone.
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

import { ApiError } from "@/shared/api";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { useRecentSolutionReviews } from "./queries";

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
);

beforeEach(() => {
  resetApiMock();
});

describe("useRecentSolutionReviews (real)", () => {
  it("maps createdAt and timeComplexity to the block model", async () => {
    apiMock.get.mockResolvedValue({
      items: [
        {
          reviewId: "r1",
          submissionId: "s1",
          problemId: "p1",
          problemTitle: "Two Sum",
          verdict: "OPTIMAL",
          timeComplexity: "O(n)",
          createdAt: "2026-11-12T03:00:00Z",
        },
      ],
    });

    const { result } = renderHook(() => useRecentSolutionReviews(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(apiMock.get).toHaveBeenCalledWith("/ai/me/solution-reviews/recent", undefined);
    expect(result.current.data).toEqual([
      {
        submissionId: "s1",
        problemTitle: "Two Sum",
        reviewedAt: "2026-11-12T03:00:00Z",
        complexity: { actualTime: "O(n)" },
        verdict: "OPTIMAL",
      },
    ]);
  });

  it("surfaces an AI failure as a query error of this block only", async () => {
    apiMock.get.mockRejectedValue(new ApiError("AIR-501", 503));

    const { result } = renderHook(() => useRecentSolutionReviews(), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
