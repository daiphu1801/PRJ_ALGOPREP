// INS0101 against the REAL api path: #19 has no absent or completion figures (F4 #56 is not built), so
// the two blocks that would show them are not drawn rather than filled with invented zeros.
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { InstructorOverviewView } from "./instructor-overview-view";

const m = viMessages.instructorOverview;

describe("InstructorOverviewView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/identity/classes/my"
        ? {
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
          }
        : Promise.reject(new Error("not implemented")),
    );
  });

  it("draws no absent gauge and no completion bars without F4 figures", async () => {
    render(withTestProviders(<InstructorOverviewView />));

    expect(
      (await screen.findAllByText(/Lập trình Java K21/)).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByText(m.students.title)).not.toBeInTheDocument();
    expect(screen.queryByText("0%")).not.toBeInTheDocument();
  });
});
