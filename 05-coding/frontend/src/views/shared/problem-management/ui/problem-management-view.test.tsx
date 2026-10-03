// Toasts on the problem list: delete result and explicit search (not per keystroke).
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ProblemManagementView } from "./problem-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

describe("ProblemManagementView toasts", () => {
  it("toasts after confirming a delete", () => {
    const toasts = renderHook(() => useToasts());
    render(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getAllByRole("button", { name: /^deleteProblem/ })[0]!);
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/);
  });

  it("stays silent while typing and toasts the count on Enter", () => {
    const toasts = renderHook(() => useToasts());
    render(<ProblemManagementView basePath="/admin/problems" />);
    const box = screen.getByLabelText("searchLabel");
    fireEvent.change(box, { target: { value: "zzzz-no-match" } });
    expect(toasts.result.current).toHaveLength(0);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info", message: "toast.searchEmpty" });
  });
});
