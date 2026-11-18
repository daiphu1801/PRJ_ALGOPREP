import { beforeEach, expect, it, vi } from "vitest";
const get = vi.fn();
vi.mock("@/shared/api", async (original) => ({
  ...(await original<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));
import { ApiError } from "@/shared/api";
import { getRealMySubmissions, getRealSubmission } from "./real-submissions";
beforeEach(() => {
  get.mockReset();
});
it("discards sample output and the actual testcase id for HIDDEN cases", async () => {
  get.mockResolvedValue({
    submissionId: "s",
    status: "WRONG_ANSWER",
    problem: null,
    testcaseDetail: {
      available: true,
      unavailableReason: null,
      items: [
        {
          order: 2,
          testcaseId: "private-id",
          visibility: "HIDDEN",
          verdict: "WA",
          runtimeMs: 3,
          memoryKb: 4,
          actualOutput: "PRIVATE",
        },
      ],
    },
  });
  const result = await getRealSubmission("s", "student");
  expect(result?.testcases[0]).toEqual({
    testcaseId: "hidden-2",
    order: 2,
    visibility: "HIDDEN",
    verdict: "WA",
    runtimeMs: 3,
    memoryKb: 4,
  });
  expect(result?.problemWithdrawn).toBe(true);
  expect(JSON.stringify(result)).not.toContain("PRIVATE");
});
it("uses the scoped instructor endpoint and preserves its existence-hiding 404", async () => {
  get.mockRejectedValue(new ApiError("JDG-110", 404));
  expect(await getRealSubmission("s", "instructor")).toBeNull();
  expect(get).toHaveBeenCalledWith("/judge/instructor/submissions/s");
});
it("uses authoritative server pagination and maps the search parameter", async () => {
  get.mockResolvedValue({ items: [], page: 2, pageSize: 20, totalCount: 31 });
  expect(
    await getRealMySubmissions({
      query: "sum",
      page: 2,
      pageSize: 50,
      verdict: "ALL",
      language: "JAVA",
    }),
  ).toEqual({ items: [], currentPage: 2, pageSize: 20, totalItems: 31 });
  expect(get).toHaveBeenCalledWith("/judge/me/submissions", {
    q: "sum",
    page: 2,
    verdict: "ALL",
    language: "JAVA",
  });
});
