// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassAssignProblemsView } from "./class-assign-problems-view";

let params = new URLSearchParams();
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ replace: vi.fn(), push }),
  usePathname: () => "/instructor/assignments/new",
}));

describe("ClassAssignProblemsView (mock data)", () => {
  beforeEach(() => {
    params = new URLSearchParams("classId=c1");
    push.mockReset();
  });

  it("dims what class c1 already has and lets the rest be picked", async () => {
    render(withTestProviders(<ClassAssignProblemsView />));

    // Two Sum is already in c1; Binary Search is only in the bank.
    await screen.findByText("Binary Search");
    expect(screen.getAllByText("Đã giao").length).toBeGreaterThan(0);
    expect(
      screen.getByRole("checkbox", { name: "Chọn Two Sum" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("checkbox", { name: "Chọn Binary Search" }),
    ).toBeEnabled();
  });

  it("shows the bottom bar with the class name and goes back after assigning", async () => {
    render(withTestProviders(<ClassAssignProblemsView />));
    await screen.findByText("Binary Search");

    fireEvent.click(
      screen.getByRole("checkbox", { name: "Chọn Binary Search" }),
    );
    expect(
      await screen.findByRole("region", {
        name: /Đã chọn 1 bài, giao cho lớp/,
      }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Giao 1 bài" }));
    await waitFor(() =>
      expect(push).toHaveBeenCalledWith("/instructor/assignments?classId=c1"),
    );
  });
});
