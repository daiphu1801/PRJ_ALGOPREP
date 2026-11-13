// Hooks of #4 to #6 in real mode: cache keys that a first report invalidates, and the two read-only
// hooks. Only the HTTP layer is faked.
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
import {
  useReviewQuickInsight,
  useReviewedProblemIds,
  useSolutionReview,
} from "./queries";

const SUBMISSION = "6f1c2b0e-0000-4000-8000-000000000001";

function setup() {
  const client = new QueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

beforeEach(() => {
  resetApiMock();
});

describe("useSolutionReview (real)", () => {
  it("invalidates the label, quick-insight and dashboard caches after a report, not its own", async () => {
    apiMock.post.mockResolvedValue({
      submissionId: SUBMISSION,
      problemId: "p1",
      language: "JAVA",
      createdAt: "2026-11-12T03:00:00Z",
      readabilityScore: 3,
      schemaVersion: "1",
      approachTitle: "x",
      verdict: "OPTIMAL",
      isApproachOptimal: true,
      complexity: {},
      strengths: [],
      improvements: [],
      edgeCases: [],
      followUpTopics: [],
      codeDiff: null,
    });
    const { client, wrapper } = setup();
    client.setQueryData(["solution-review", "problem-ids"], new Set());
    client.setQueryData(["solution-review", "quick-insight", "p1"], null);
    client.setQueryData(["student-dashboard", "recent-reviews"], []);

    const { result } = renderHook(() => useSolutionReview(SUBMISSION, "ready"), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    for (const key of [
      ["solution-review", "problem-ids"],
      ["solution-review", "quick-insight", "p1"],
      ["student-dashboard", "recent-reviews"],
    ]) {
      expect(client.getQueryState(key)?.isInvalidated).toBe(true);
    }
    expect(client.getQueryState(["solution-review", SUBMISSION, "ready"])?.isInvalidated).toBe(false);
    expect(apiMock.post).toHaveBeenCalledTimes(1);
  });

  it("invalidates nothing when the call fails", async () => {
    apiMock.post.mockRejectedValue(new ApiError("AIR-501", 503));
    const { client, wrapper } = setup();
    client.setQueryData(["solution-review", "problem-ids"], new Set());

    const { result } = renderHook(() => useSolutionReview(SUBMISSION, "ready"), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(client.getQueryState(["solution-review", "problem-ids"])?.isInvalidated).toBe(false);
  });
});

describe("useReviewedProblemIds and useReviewQuickInsight (real)", () => {
  it("exposes the ids as a Set", async () => {
    apiMock.get.mockResolvedValue({ problemIds: ["p1"] });
    const { wrapper } = setup();

    const { result } = renderHook(() => useReviewedProblemIds(), { wrapper });
    await waitFor(() => expect(result.current.data?.has("p1")).toBe(true));
  });

  it("stays empty (no throw) when F5 fails, so the problem list keeps rendering", async () => {
    apiMock.get.mockRejectedValue(new ApiError("AIR-501", 503));
    const { wrapper } = setup();

    const { result } = renderHook(() => useReviewedProblemIds(), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("does not call without a problem id", () => {
    const { wrapper } = setup();

    renderHook(() => useReviewQuickInsight(undefined), { wrapper });
    expect(apiMock.get).not.toHaveBeenCalled();
  });
});
