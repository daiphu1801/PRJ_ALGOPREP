// Toasts on the problem list: delete result and explicit search (not per keystroke).
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ProblemManagementView } from "./problem-management-view";

const push = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => {
  push.mockClear();
  act(() => toast.clear());
});

// The list loads through TanStack Query, so every render waits for the toolbar to appear.
async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findByLabelText("searchLabel");
}

describe("ProblemManagementView toasts", () => {
  it("toasts after confirming a delete", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(
      screen.getAllByRole("button", { name: /^deleteProblem/ })[0]!,
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/);
  });

  it("asks for confirmation before a bulk delete, and deletes nothing on cancel", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!,
    );
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));

    // The click only opens the dialog: nothing is deleted yet.
    const dialog = screen.getByRole("dialog");
    expect(toasts.result.current).toHaveLength(0);

    fireEvent.click(within(dialog).getByRole("button", { name: "cancel" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(toasts.result.current).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );
    expect(toasts.result.current.at(-1)?.message).toMatch(
      /^toast\.bulk\.delete/,
    );
  });

  it("steps back a page when deleting the last rows of the last page", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const lastPage = screen
      .getAllByRole("button", { name: /^goToPage/ })
      .at(-1)!;
    fireEvent.click(lastPage);
    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );

    // Without the clamp this page would be empty and read "page 3 of 2".
    expect(
      screen.getAllByRole("checkbox", { name: /^selectRow/ }).length,
    ).toBeGreaterThan(0);
  });

  it("warns in the bulk delete dialog when the filter hides some selected rows", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!,
    );
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "zzzz-no-match" },
    });
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(
      within(screen.getByRole("dialog")).getByText(/^confirmBulkDeleteHidden/),
    ).toBeInTheDocument();
  });

  it("says so when a bulk action is not wired to an API", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(
      screen.getAllByRole("checkbox", { name: /^selectRow/ })[0]!,
    );
    fireEvent.click(screen.getByRole("button", { name: "bulk.exportCsv" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "info" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.notWired/);
  });

  it("opens the authoring form pre-filled for exactly one selected problem (F2-16)", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const rows = screen.getAllByRole("checkbox", { name: /^selectRow/ });
    fireEvent.click(rows[0]!);
    fireEvent.click(rows[1]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.duplicate" }));
    expect(push).not.toHaveBeenCalled();
    expect(toasts.result.current.at(-1)?.message).toBe(
      "toast.duplicatePickOne",
    );

    fireEvent.click(rows[1]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.duplicate" }));
    expect(push).toHaveBeenCalledTimes(1);
    expect(push.mock.calls[0]![0]).toMatch(/^\/admin\/problems\/new\?from=/);
  });

  it("warns about submissions and classes when deleting one problem, without blocking", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "Coin Change" },
    });
    fireEvent.click(
      screen.getAllByRole("button", { name: /^deleteProblem/ })[0]!,
    );
    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getByText(/^confirmDeleteImpact/).textContent,
    ).toContain('"submissions":2540,"classes":3');
    expect(
      within(dialog).getByRole("button", { name: "delete" }),
    ).toBeEnabled();
  });

  it("summarises submissions and assigned problems in the bulk delete dialog", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "Coin Change" },
    });
    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    expect(
      within(screen.getByRole("dialog")).getByText(/^confirmBulkDeleteImpact/)
        .textContent,
    ).toContain('"count":1,"submissions":2540,"assigned":1');
  });

  it("stays silent while typing and toasts the count on Enter", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const box = screen.getByLabelText("searchLabel");
    fireEvent.change(box, { target: { value: "zzzz-no-match" } });
    expect(toasts.result.current).toHaveLength(0);
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "info",
      message: "toast.searchEmpty",
    });
  });
});
