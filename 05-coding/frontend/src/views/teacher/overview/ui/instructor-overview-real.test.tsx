// INS0101 against the REAL api path: the class blocks read F1 #56; when F4 is down (IDT-507) it falls
// back to #19, which has no absent or completion figures, so the blocks that would show them are not
// drawn rather than filled with invented zeros. Recent submissions read F4 #13; the daily chart is hidden.
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InstructorOverviewView } from "./instructor-overview-view";

const m = viMessages.instructorOverview;

describe("InstructorOverviewView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === "/identity/instructor/classes/summary") {
        return {
          items: [
            {
              classId: "c1",
              name: "Lập trình Java K21",
              description: "",
              schedule: null,
              studentCount: 12,
              completionPercent: 40,
              avgScore: 7.5,
              absentCount: 3,
              createdAt: "2026-11-01T03:00:00Z",
            },
          ],
        };
      }
      if (path === "/identity/classes/my") {
        return {
          items: [
            {
              id: "c1",
              name: "Lập trình Java K21",
              description: "",
              schedule: null,
              studentCount: 12,
              createdAt: "2026-11-01T03:00:00Z",
            },
          ],
        };
      }
      // Quiet defaults, so a test that breaks one block sees exactly one error state.
      if (path === "/problem-bank/instructor/assigned-problems/top") {
        return { items: [] };
      }
      if (path === "/judge/instructor/submissions") {
        return { page: 1, pageSize: 20, totalCount: 0, items: [] };
      }
      return Promise.reject(new Error("not implemented"));
    });
  });

  it("words the class feed from the activity type and the actor name (#45)", async () => {
    const base = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/identity/instructor/dashboard/recent-activity"
        ? {
            items: [
              {
                id: "a1",
                activityType: "SUBMISSION_ACCEPTED",
                actorUserId: "u1",
                actorDisplayName: "Nguyễn Văn An",
                scopeClassId: "c1",
                refType: "SUBMISSION",
                refId: "s1",
                occurredAt: new Date(Date.now() - 5 * 60_000).toISOString(),
              },
            ],
          }
        : base(path),
    );

    render(withTestProviders(<InstructorOverviewView />));

    expect(
      await screen.findByText(
        m.activity.type.SUBMISSION_ACCEPTED.replace("{name}", "Nguyễn Văn An"),
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("5 phút trước")).toBeInTheDocument();
  });

  it("keeps the rest of the screen when the feed call fails", async () => {
    render(withTestProviders(<InstructorOverviewView />));

    expect(
      (await screen.findAllByText(/Lập trình Java K21/)).length,
    ).toBeGreaterThan(0);
    expect(screen.getByText(m.activity.title)).toBeInTheDocument();
  });

  it("draws the absent gauge and completion bars from #56", async () => {
    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText(m.students.title)).toBeInTheDocument();
    expect(screen.getByText("40%")).toBeInTheDocument();
    expect(screen.getByText("3 vắng")).toBeInTheDocument();
    expect(apiMock.get).not.toHaveBeenCalledWith("/identity/classes/my");
  });

  it("draws no absent gauge and no completion bars when F4 is down (IDT-507 falls back to #19)", async () => {
    const base = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/identity/instructor/classes/summary"
        ? Promise.reject(new ApiError("IDT-507", 503, "down"))
        : base(path),
    );
    render(withTestProviders(<InstructorOverviewView />));

    expect(
      (await screen.findAllByText(/Lập trình Java K21/)).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByText(m.students.title)).not.toBeInTheDocument();
    expect(screen.queryByText("0%")).not.toBeInTheDocument();
  });

  it("lists the top assigned problems from #54 with their real submission counts", async () => {
    const base = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/problem-bank/instructor/assigned-problems/top"
        ? {
            items: [
              { problemId: "p1", title: "Hai tổng", submissionCount: 1234 },
              { problemId: "p2", title: "Chuỗi đối xứng", submissionCount: 7 },
            ],
          }
        : base(path),
    );

    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText("Hai tổng")).toBeInTheDocument();
    expect(screen.getByText("1.234")).toBeInTheDocument();
    expect(screen.getByText("Chuỗi đối xứng")).toBeInTheDocument();
    expect(apiMock.get).not.toHaveBeenCalledWith(
      "/problem-bank/instructor/class-assignments",
      expect.anything(),
    );
  });

  it("shows the empty sentence when #54 answers an empty list", async () => {
    const base = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/problem-bank/instructor/assigned-problems/top"
        ? { items: [] }
        : base(path),
    );

    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText(m.assignments.title)).toBeInTheDocument();
    expect((await screen.findAllByText(m.empty)).length).toBeGreaterThan(0);
    expect(screen.queryByText(viMessages.common.error)).not.toBeInTheDocument();
  });

  it("shows an error with retry, not the empty sentence, when #54 fails", async () => {
    const base = apiMock.get.getMockImplementation()!;
    let calls = 0;
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/instructor/assigned-problems/top") {
        calls += 1;
        return calls === 1
          ? Promise.reject(new Error("down"))
          : {
              items: [
                { problemId: "p1", title: "Hai tổng", submissionCount: 3 },
              ],
            };
      }
      return base(path);
    });

    render(withTestProviders(<InstructorOverviewView />));

    expect(
      await screen.findByText(viMessages.common.error),
    ).toBeInTheDocument();
    expect(screen.queryByText("Hai tổng")).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: viMessages.common.retry }),
    );
    expect(await screen.findByText("Hai tổng")).toBeInTheDocument();
  });

  it("lists the five newest submissions from F4 #13 and hides the daily chart", async () => {
    const base = apiMock.get.getMockImplementation()!;
    const item = (n: number) => ({
      submissionId: `s${n}`,
      studentId: `u${n}`,
      studentName: `Học viên ${n}`,
      classId: "c1",
      className: "Lập trình Java K21",
      problemTitle: `Bài ${n}`,
      language: "PYTHON",
      status: "ACCEPTED",
      passedCount: 5,
      totalCount: 5,
      submittedAt: "2026-11-06T03:00:00Z",
      leftClass: n === 1,
    });
    apiMock.get.mockImplementation(async (path: string, params?: unknown) =>
      path === "/judge/instructor/submissions"
        ? {
            page: 1,
            pageSize: 20,
            totalCount: 6,
            items: [1, 2, 3, 4, 5, 6].map(item),
          }
        : base(path, params),
    );

    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText("Học viên 1")).toBeInTheDocument();
    expect(screen.getByText("Học viên 5")).toBeInTheDocument();
    expect(screen.queryByText("Học viên 6")).not.toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith("/judge/instructor/submissions", {
      result: "all",
      q: "",
      page: 1,
    });
    expect(screen.queryByText(m.chart.title)).not.toBeInTheDocument();
    // The mock roster is not read in real mode.
    expect(apiMock.get).not.toHaveBeenCalledWith(
      expect.stringContaining("/students"),
      expect.anything(),
    );
  });

  it("shows an error with retry in the recent-submissions block when F4 #13 fails", async () => {
    const base = apiMock.get.getMockImplementation()!;
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/judge/instructor/submissions"
        ? Promise.reject(new Error("down"))
        : base(path),
    );
    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText(m.submissions.title)).toBeInTheDocument();
    expect(
      await screen.findByText(viMessages.common.error),
    ).toBeInTheDocument();
    expect(screen.queryByText(m.submissions.empty)).not.toBeInTheDocument();
  });
});
