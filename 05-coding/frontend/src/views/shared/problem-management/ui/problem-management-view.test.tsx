// Toasts on the problem list: delete result and explicit search (not per keystroke).
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ProblemManagementView } from "./problem-management-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

// The list loads through TanStack Query, so every render waits for the toolbar to appear.
async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findByLabelText("searchLabel");
}

describe("ProblemManagementView toasts", () => {
  it("toasts after confirming a delete", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getAllByRole("button", { name: /^deleteProblem/ })[0]!);
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/);
  });

  it("asks for confirmation before a bulk delete, and deletes nothing on cancel", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));

    // The click only opens the dialog: nothing is deleted yet.
    const dialog = screen.getByRole("dialog");
    expect(toasts.result.current).toHaveLength(0);

    fireEvent.click(within(dialog).getByRole("button", { name: "cancel" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(toasts.result.current).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.bulk\.delete/);
  });

  it("steps back a page when deleting the last rows of the last page", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const lastPage = screen.getAllByRole("button", { name: /^goToPage/ }).at(-1)!;
    fireEvent.click(lastPage);
    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }));

    // Without the clamp this page would be empty and read "page 3 of 2".
    expect(screen.getAllByRole("checkbox", { name: /^selectRow/ }).length).toBeGreaterThan(0);
  });

  it("warns in the bulk delete dialog when the filter hides some selected rows", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    fireEvent.change(screen.getByLabelText("searchLabel"), { target: { value: "zzzz-no-match" } });
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(within(screen.getByRole("dialog")).getByText(/^confirmBulkDeleteHidden/)).toBeInTheDocument();
  });

  it("says so when a bulk action is not wired to an API", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.duplicate" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.notWired/);
  });

  it("stays silent while typing and toasts the count on Enter", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const box = screen.getByLabelText("searchLabel");
    fireEvent.change(box, { target: { value: "zzzz-no-match" } });
    expect(toasts.result.current).toHaveLength(0);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info", message: "toast.searchEmpty" });
  });
});
