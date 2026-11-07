// View-level test of INS0204 against the REAL api path (03-dd/api/F1_identity.md #28): only the HTTP
// layer is faked. What matters: the profile and status are the server's, the table lists the problems
// still given to the class with THIS student's best result, and a student who is not in the class
// reads as "not found".
import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("next/navigation", () => ({
  useRouter: () => ({ back: vi.fn(), push: vi.fn() }),
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassStudentDetailView } from "./class-student-detail-view";

const m = viMessages.classStudentDetail;
const DETAIL = "/identity/classes/c1/students/s1/detail";

const PROFILE = {
  classId: "c1",
  studentId: "s1",
  displayName: "Trần Bình",
  email: "binh@x.vn",
  phone: null,
  className: "Lớp A",
  joinedAt: "2026-08-01T00:00:00Z",
  status: "ABSENT",
  assignmentProgress: [
    { problemId: "p1", bestRatio: 1, bestVerdict: "ACCEPTED" },
    { problemId: "p2", bestRatio: null, bestVerdict: null },
  ],
};

function serve(detail: () => Promise<unknown>) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === DETAIL) return detail();
    if (path === "/identity/classes/my") return { items: [] };
    if (path === "/problem-bank/instructor/class-assignments")
      return {
        items: [
          {
            problemId: "p1",
            title: "Tổng hai số",
            topicNames: ["Mảng"],
            levelCode: "EASY",
            assignedClasses: [{ classId: "c1", className: "Lớp A" }],
            submissionCount: 3,
            acRatePercent: 40,
            withdrawn: false,
          },
        ],
        page: { number: 1, size: 20, totalItems: 1, totalPages: 1 },
      };
    return { items: [], page: 1, pageSize: 20, totalCount: 0 };
  });
}

describe("ClassStudentDetailView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
  });

  it("shows the profile, the status and the best result of this student per given problem", async () => {
    serve(async () => PROFILE);
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    expect((await screen.findAllByText("Trần Bình")).length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "binh@x.vn" })).toBeInTheDocument();
    expect(screen.getByText(m.info.statusValue.absent)).toBeInTheDocument();
    const table = await screen.findByRole("table", {
      name: m.assignmentProgress.title,
    });
    expect(
      within(table).getByText(m.assignmentProgress.col.best),
    ).toBeInTheDocument();
    expect(await within(table).findByText("Tổng hai số")).toBeInTheDocument();
    expect(within(table).getByText(/^100% · /)).toBeInTheDocument();
    // The second problem has no submission yet: no title from the first page, no result.
    expect(within(table).getAllByText("-").length).toBeGreaterThanOrEqual(2);
  });

  it("says not found when the student is not an active member (IDT-414)", async () => {
    serve(async () => {
      throw new ApiError("IDT-414", 404, "gone");
    });
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    expect(await screen.findByText(m.notFound)).toBeInTheDocument();
  });

  const row = (id: string, title: string) => ({
    problemId: id,
    title,
    topicNames: ["Mảng"],
    levelCode: "EASY",
    assignedClasses: [{ classId: "c1", className: "Lớp A" }],
    submissionCount: 1,
    acRatePercent: 50,
    withdrawn: false,
  });
  const THREE = {
    ...PROFILE,
    assignmentProgress: ["p1", "p2", "p3"].map((problemId) => ({
      problemId,
      bestRatio: null,
      bestVerdict: null,
    })),
  };

  it("names a problem that sits on page 2 or 3 of #49, not only the first page", async () => {
    apiMock.get.mockImplementation(
      async (path: string, params?: { page?: number }) => {
        if (path === DETAIL) return THREE;
        if (path === "/problem-bank/instructor/class-assignments") {
          const page = params?.page ?? 1;
          return {
            items: [row(`p${page}`, `Bài trang ${page}`)],
            page: { number: page, size: 1, totalItems: 3, totalPages: 3 },
          };
        }
        return { items: [], page: 1, pageSize: 20, totalCount: 0 };
      },
    );
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    const table = await screen.findByRole("table", {
      name: m.assignmentProgress.title,
    });
    expect(await within(table).findByText("Bài trang 3")).toBeInTheDocument();
    expect(within(table).getByText("Bài trang 1")).toBeInTheDocument();
    expect(within(table).getByText("Bài trang 2")).toBeInTheDocument();
    const pages = apiMock.get.mock.calls
      .filter(([path]) => path === "/problem-bank/instructor/class-assignments")
      .map(([, params]) => (params as { page: number }).page);
    expect(pages).toEqual([1, 2, 3]);
  });

  it("keeps the table with placeholder titles when a later page of #49 fails", async () => {
    apiMock.get.mockImplementation(
      async (path: string, params?: { page?: number }) => {
        if (path === DETAIL) return THREE;
        if (path === "/problem-bank/instructor/class-assignments") {
          if (params?.page === 2) throw new ApiError("PRB-V01", 400, "bad");
          return {
            items: [row("p1", "Bài trang 1")],
            page: { number: 1, size: 1, totalItems: 3, totalPages: 3 },
          };
        }
        return { items: [], page: 1, pageSize: 20, totalCount: 0 };
      },
    );
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    const table = await screen.findByRole("table", {
      name: m.assignmentProgress.title,
    });
    expect(within(table).getAllByText("-").length).toBeGreaterThanOrEqual(3);
    expect(within(table).queryByText("Bài trang 1")).not.toBeInTheDocument();
  });
});
