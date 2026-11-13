// View-level test of INS0203 against the REAL api path (03-dd/api/F1_identity.md #59): only the HTTP
// layer is faked. What matters: the server's page is shown as is (status label, completion, average,
// a dash for no score), and class, search text and page go to the server as query parameters.
import { render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams("classId=c1&page=2&q=an"),
  useRouter: () => ({ replace }),
  usePathname: () => "/instructor/students",
}));

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ClassProgressView } from "./class-progress-view";

const m = viMessages.classProgress;

const ROW = {
  classId: "c1",
  className: "Lớp A",
  studentId: "s1",
  displayName: "Nguyễn Văn An",
  joinedAt: "2026-08-01T00:00:00Z",
  completionPercent: 50,
  avgScore: 7.5,
  status: "ABSENT",
  streakDays: 0,
  trendPoints: [null, null, null, null, 8, 6],
  lastSubmittedAt: null,
};

describe("ClassProgressView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockImplementation(async (path: string) =>
      path === "/identity/instructor/students"
        ? {
            items: [
              ROW,
              {
                ...ROW,
                studentId: "s2",
                displayName: "Trần Bình",
                avgScore: null,
                completionPercent: 0,
                status: "INSUFFICIENT_DATA",
                trendPoints: [null, null, null, null, null, null],
              },
            ],
            page: { page: 2, pageSize: 20, totalCount: 22, totalPages: 2 },
          }
        : { items: [] },
    );
  });

  it("shows the server's page and sends class, search and page as parameters", async () => {
    render(withTestProviders(<ClassProgressView />));

    const table = await screen.findByRole("table", { name: m.header.title });
    expect(await within(table).findByText("Nguyễn Văn An")).toBeInTheDocument();
    expect(within(table).getByText(m.status.absent)).toBeInTheDocument();
    expect(
      within(table).getByText(m.status.insufficient_data),
    ).toBeInTheDocument();
    expect(within(table).getByText("50%")).toBeInTheDocument();
    expect(within(table).getByText("7.5")).toBeInTheDocument();
    // A student with no graded submission shows a dash, not 0.0.
    expect(within(table).queryByText("0.0")).toBeNull();
    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(
        "/identity/instructor/students",
        {
          classId: "c1",
          q: "an",
          page: 2,
          pageSize: 20,
        },
      ),
    );
  });
});
