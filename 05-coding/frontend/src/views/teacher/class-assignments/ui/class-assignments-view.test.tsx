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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassAssignmentsView } from "./class-assignments-view";

// The list state lives in the URL (?classId=&q=&page=): the tests drive it through these params.
let params = new URLSearchParams();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ replace: vi.fn() }),
  usePathname: () => "/instructor/assignments",
}));

describe("ClassAssignmentsView", () => {
  beforeEach(() => {
    params = new URLSearchParams();
  });

  it("shows the first topic alphabetically plus +n, with every topic in the tooltip (Q5)", async () => {
    render(withTestProviders(<ClassAssignmentsView />));

    const cell = await screen.findByText("Bảng băm +1");
    expect(cell).toHaveAttribute("title", "Bảng băm, Mảng");
  });

  it("shows the figures of the selected class, and the totals on the All tab (Q11)", async () => {
    render(withTestProviders(<ClassAssignmentsView />));
    expect(await screen.findByText("512")).toBeInTheDocument();

    params = new URLSearchParams("classId=c1");
    render(withTestProviders(<ClassAssignmentsView />));
    // Two Sum in class c1: a third of the 512 submissions in the mock split.
    expect(await screen.findByText("171")).toBeInTheDocument();
  });

  it("offers the class choice in the removal dialog only on the All tab (one class per removal)", async () => {
    render(withTestProviders(<ClassAssignmentsView />));
    await screen.findByText("Two Sum");
    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Gỡ khỏi lớp" }))[0]!,
    );
    expect(
      within(await screen.findByRole("dialog")).getByRole("combobox"),
    ).toBeInTheDocument();
  });

  it("loads assigned problems and renders the table", async () => {
    render(withTestProviders(<ClassAssignmentsView />));

    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(
      (await screen.findAllByText("Bài giao cho lớp")).length,
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

  it("assigns a bank problem to a class from the popup", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassAssignmentsView />));

    await screen.findByText("Two Sum");
    expect(screen.queryByText("Binary Search")).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "+ Gán từ ngân hàng bài toán" }),
    );
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(await within(dialog).findByLabelText(/Binary Search/));
    fireEvent.click(within(dialog).getByRole("button", { name: "Giao 1 bài" }));

    expect(await screen.findByText("Binary Search")).toBeInTheDocument();
    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.tone)).toContain(
        "success",
      ),
    );
  });
});
