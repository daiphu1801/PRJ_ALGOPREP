import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (original) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await original<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { MyClassesView } from "./my-classes-view";

const klass = {
  classId: "class-a",
  name: "Java K21",
  description: "",
  schedule: null,
  instructorName: "Teacher",
  joinedAt: "2026-11-01T03:00:00Z",
};

function serve(solvedCount: number | null) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/identity/me/classes") return { items: [klass] };
    if (path === "/problem-bank/me/class-problems")
      return {
        // One returned row deliberately differs from the total of 60 across all pages.
        items: [
          {
            problemId: "p1",
            title: "One row",
            code: "P1",
            levelCode: "EASY",
            topicNames: [],
            withdrawn: false,
            solveState: "SOLVED",
            assignedAt: "2026-11-01T00:00:00Z",
          },
        ],
        page: { number: 1, size: 20, totalItems: 60, totalPages: 3 },
        meta: { totalAssigned: 60, solvedCount },
      };
    throw new Error(`Unexpected route ${path}`);
  });
}

beforeEach(resetApiMock);

describe("whole-class progress on learner class cards", () => {
  it("uses meta across all pages and makes one progress read per class", async () => {
    serve(37);
    render(withTestProviders(<MyClassesView />));
    expect(await screen.findByText("Đã xong 37/60 bài")).toBeInTheDocument();
    await waitFor(() =>
      expect(
        apiMock.get.mock.calls.filter(
          ([path]) => path === "/problem-bank/me/class-problems",
        ),
      ).toHaveLength(1),
    );
    expect(apiMock.get).toHaveBeenCalledWith(
      "/problem-bank/me/class-problems",
      { classId: "class-a", page: 1 },
    );
  });

  it("shows total only when solvedCount is unknown instead of inventing zero solved", async () => {
    serve(null);
    render(withTestProviders(<MyClassesView />));
    expect(await screen.findByText("60 bài được giao")).toBeInTheDocument();
    expect(screen.queryByText(/Đã xong 0/)).toBeNull();
    expect(screen.queryByRole("progressbar")).toBeNull();
  });
});
