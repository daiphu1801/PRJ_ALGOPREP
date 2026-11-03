// View-level tests of USR0702 against the REAL api path (03-dd/api/F1_identity.md #25B): only the HTTP
// layer is faked. What matters: the class comes from its own call with no invented assignment progress,
// and a class that is not the learner's reads as not found.
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

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

describe("MyClassDetailView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
  });

  it("shows the class by its own call and leaves out the assignments and the progress", async () => {
    apiMock.get.mockResolvedValue(MINE);
    render(withTestProviders(<MyClassDetailView classId="c1" />));

    expect(
      await screen.findByRole("heading", { name: MINE.name }),
    ).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith("/identity/me/classes/c1");
    expect(screen.getByText("Thầy Nam")).toBeInTheDocument();
    expect(screen.queryByText(m.detail.progress)).toBeNull();
    expect(screen.queryByText(m.card.assignments)).toBeNull();
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
