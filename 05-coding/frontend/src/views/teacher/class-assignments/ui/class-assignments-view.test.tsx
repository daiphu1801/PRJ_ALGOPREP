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
import { ClassAssignmentsView } from "./class-assignments-view";

describe("ClassAssignmentsView", () => {
  it("loads assigned problems and renders the table", async () => {
    render(withTestProviders(<ClassAssignmentsView />));

    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(
      (await screen.findAllByText("Bài tập của tôi")).length,
    ).toBeGreaterThan(0);
  });

  it("raises a success toast after removing an assignment", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassAssignmentsView />));

    await screen.findByText("Two Sum");
    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Gỡ khỏi lớp" }))[0]!,
    );
    fireEvent.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Gỡ khỏi lớp",
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
