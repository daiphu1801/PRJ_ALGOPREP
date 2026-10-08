// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassManagementView } from "./class-management-view";

describe("ClassManagementView", () => {
  it("renders the class grid and no student table", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect(await screen.findByText("Lập trình Java K21")).toBeInTheDocument();
    // The student table moved to class_progress on 2026-09-27 — this screen manages classes only.
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("raises a success toast after deleting a class", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassManagementView />));

    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Xoá lớp" }))[0]!,
    );
    fireEvent.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Xoá lớp",
      }),
    );

    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.tone)).toEqual([
        "success",
      ]),
    );
    expect(toasts.result.current[0]!.message).not.toMatch(/^toast\./);
  });
});
