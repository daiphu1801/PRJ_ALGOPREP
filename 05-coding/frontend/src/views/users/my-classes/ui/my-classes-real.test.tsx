// View-level tests of USR0701 against the REAL api path (03-dd/api/F1_identity.md #25, #25A),
// only the HTTP layer is faked. What matters: the learner's classes are the server's, with no invented
// assignment progress, and a join refusal reads as its own code (wrong code, too many tries).
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { MyClassesView } from "./my-classes-view";

const m = viMessages.myClasses;
const apiErrors = viMessages.apiErrors as Record<string, string>;

const MINE = {
  classId: "c1",
  name: "Lập trình Java K21",
  description: "",
  schedule: { days: [1, 3], startTime: "19:00", endTime: "21:00" },
  instructorName: "Thầy Nam",
  joinedAt: "2026-11-01T03:00:00Z",
};

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

describe("MyClassesView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    apiMock.get.mockResolvedValue({ items: [MINE] });
  });

  it("lists the learner's classes with the instructor and the join date, and no progress bar", async () => {
    render(withTestProviders(<MyClassesView />));

    expect(await screen.findByText(MINE.name)).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith("/identity/me/classes");
    expect(screen.getByText("Thầy Nam")).toBeInTheDocument();
    expect(screen.getByText("01/11/2026")).toBeInTheDocument();
    expect(screen.queryByText(/Đã xong/)).toBeNull();
  });

  it("joins by code, toasts the class name and reloads the list", async () => {
    apiMock.post.mockResolvedValue({ classId: "c2", className: "Lớp mới" });
    const toasts = watch();
    render(withTestProviders(<MyClassesView />));
    await screen.findByText(MINE.name);
    const before = apiMock.get.mock.calls.length;

    fireEvent.change(screen.getByLabelText(m.join.label), {
      target: { value: "ABCD2345" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.join.submit }));

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith("/identity/classes/join", {
        code: "ABCD2345",
      }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "success",
        m.join.success.replace("{name}", "Lớp mới"),
      ]),
    );
    await waitFor(() =>
      expect(apiMock.get.mock.calls.length).toBeGreaterThan(before),
    );
  });

  it("reads a wrong code and a rate limit each by their own code", async () => {
    apiMock.post.mockRejectedValueOnce(new ApiError("IDT-410", 422));
    apiMock.post.mockRejectedValueOnce(new ApiError("IDT-412", 429));
    const toasts = watch();
    render(withTestProviders(<MyClassesView />));
    await screen.findByText(MINE.name);
    fireEvent.change(screen.getByLabelText(m.join.label), {
      target: { value: "WRONG123" },
    });

    fireEvent.click(screen.getByRole("button", { name: m.join.submit }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-410"]]),
    );

    fireEvent.click(screen.getByRole("button", { name: m.join.submit }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["IDT-412"]]),
    );
  });
});
