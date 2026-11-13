// View-level tests of USR0702 against the REAL api path (03-dd/api/F1_identity.md #25B): only the HTTP
// layer is faked. What matters: the class comes from its own call with no invented assignment progress,
// and a class that is not the learner's reads as not found.
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
import { MyClassDetailView } from "./my-class-detail-view";

const m = viMessages.myClasses;

const MINE = {
  classId: "c1",
  name: "Lập trình Java K21",
  description: "",
  schedule: { days: [1, 3], startTime: "19:00", endTime: "21:00" },
  instructorName: "Thầy Nam",
  joinedAt: "2026-11-01T03:00:00Z",
};

function routes(
  rowOver: Record<string, unknown> = {},
  meta: { totalAssigned: number; solvedCount: number | null } = {
    totalAssigned: 21,
    solvedCount: 5,
  },
) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/identity/me/classes/c1") return MINE;
    if (path === "/problem-bank/me/class-problems")
      return {
        items: [
          {
            classId: "c1",
            problemId: "p1",
            code: "P-1",
            title: "Two Sum",
            levelDisplayName: "Dễ",
            solveState: "ATTEMPTED",
            assignedAt: "2026-11-02T03:00:00Z",
            withdrawn: false,
            ...rowOver,
          },
        ],
        page: { number: 1, size: 20, totalItems: 21, totalPages: 2 },
        meta,
      };
    throw new Error(`unexpected GET ${path}`);
  });
}

describe("MyClassDetailView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
  });

  it("shows the class by its own call, with its problems from #13A and the progress from meta", async () => {
    routes();
    render(withTestProviders(<MyClassDetailView classId="c1" />));

    expect(
      await screen.findByRole("heading", { name: MINE.name }),
    ).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith("/identity/me/classes/c1");
    expect(screen.getByText("Thầy Nam")).toBeInTheDocument();
    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith(
      "/problem-bank/me/class-problems",
      {
        classId: "c1",
        page: 1,
      },
    );
    // The ratio is the server total over all pages (meta), not the rows of this page.
    expect(
      screen.getAllByText(
        m.card.progress.replace("{solved}", "5").replace("{total}", "21"),
      ).length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "Two Sum" })).toHaveAttribute(
      "href",
      "/problems/p1",
    );
    expect(screen.getByText(m.card.attempted)).toBeInTheDocument();
  });

  it("a withdrawn problem keeps its row, flagged, and is not a link", async () => {
    routes({ withdrawn: true });
    render(withTestProviders(<MyClassDetailView classId="c1" />));
    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText(m.detail.withdrawn)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Two Sum" })).toBeNull();
  });

  it("an unknown solved count (meta.solvedCount null) shows the total alone, no bar and no ratio", async () => {
    routes({}, { totalAssigned: 21, solvedCount: null });
    render(withTestProviders(<MyClassDetailView classId="c1" />));
    expect(
      await screen.findByText(
        m.detail.progressUnknown.replace("{total}", "21"),
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Đã xong \d+\/21/)).toBeNull();
  });

  it("pages through the server: the next page asks for page 2", async () => {
    routes();
    render(withTestProviders(<MyClassDetailView classId="c1" />));
    await screen.findByText("Two Sum");
    fireEvent.click(screen.getByRole("button", { name: m.detail.next }));
    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(
        "/problem-bank/me/class-problems",
        {
          classId: "c1",
          page: 2,
        },
      ),
    );
  });

  it("a failed problem read shows the retry state of that block only; the class stays", async () => {
    routes();
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === "/identity/me/classes/c1") return MINE;
      throw new ApiError("PRB-057", 503);
    });
    render(withTestProviders(<MyClassDetailView classId="c1" />));
    expect(
      await screen.findByRole("button", { name: "Thử lại" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Thầy Nam")).toBeInTheDocument();
  });

  it("an empty class says nothing was assigned", async () => {
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/identity/me/classes/c1"
        ? MINE
        : {
            items: [],
            page: { number: 1, size: 20, totalItems: 0, totalPages: 1 },
            meta: { totalAssigned: 0, solvedCount: 0 },
          },
    );
    render(withTestProviders(<MyClassDetailView classId="c1" />));
    expect(await screen.findByText(m.card.noAssignments)).toBeInTheDocument();
  });

  it("says not found when the class is not the learner's (IDT-411) and errors on anything else", async () => {
    apiMock.get.mockRejectedValue(new ApiError("IDT-411", 404));
    const { unmount } = render(
      withTestProviders(<MyClassDetailView classId="c9" />),
    );
    expect(await screen.findByText(m.detail.notFound)).toBeInTheDocument();
    unmount();

    apiMock.get.mockRejectedValue(new ApiError("UNKNOWN_ERROR", 500));
    render(withTestProviders(<MyClassDetailView classId="c9" />));
    expect(
      await screen.findByRole("button", { name: "Thử lại" }),
    ).toBeInTheDocument();
    expect(screen.queryByText(m.detail.notFound)).toBeNull();
  });
});
