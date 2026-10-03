// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { act, fireEvent, render, renderHook, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { InstructorGradingView } from "./instructor-grading-view";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

describe("InstructorGradingView", () => {
  it("loads the pending manual-grading queue by default", async () => {
    render(withTestProviders(<InstructorGradingView />));

    expect(await screen.findByText("Trần Thị Bích")).toBeInTheDocument();
    expect((await screen.findAllByText("Chấm bài")).length).toBeGreaterThan(0);
  });

  it("raises a success toast after saving a grade", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<InstructorGradingView />));

    fireEvent.click((await screen.findAllByRole("button", { name: "Chấm ngay" }))[0]!);
    fireEvent.click(within(await screen.findByRole("dialog")).getByRole("button", { name: "Lưu điểm" }));

    await waitFor(() => expect(toasts.result.current.map((item) => item.tone)).toEqual(["success"]));
    expect(toasts.result.current[0]!.message).not.toMatch(/^toast\./);
  });
});
