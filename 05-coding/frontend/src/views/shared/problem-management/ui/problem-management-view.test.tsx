// SHR0201 in mock mode: the mock answers the same contract as the real endpoint (server-side filter, sort
// and paging), so these tests prove the screen's own behaviour: what it sends, how it reports, what it clears.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { resetAdminProblemMock } from "../api/__mock__/admin-problem-mocks";
import { ProblemManagementView } from "./problem-management-view";

const push = vi.hoisted(() => vi.fn());
const saveBlob = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/shared/lib", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  saveBlob,
}));

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
  useLocale: () => "vi",
  // The mock answer of a failed problem is its raw code, so a test can assert which one was shown.
  useApiErrorMessage: () => (error: { code?: string }) => `err:${error.code}`,
}));

beforeEach(() => {
  push.mockClear();
  saveBlob.mockClear();
  resetAdminProblemMock();
  localStorage.clear();
  act(() => toast.clear());
});

afterEach(() => localStorage.clear());

// The list loads through TanStack Query, so every render waits for the first rows to appear.
async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findAllByRole("checkbox", { name: /^selectRow/ });
}

const rowBoxes = () => screen.getAllByRole("checkbox", { name: /^selectRow/ });

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
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.deleted/),
    );
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
  });

  it("asks for confirmation before a bulk delete, and deletes nothing on cancel", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(rowBoxes()[0]!);
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
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toMatch(
        /^toast\.bulk\.delete/,
      ),
    );
  });

  it("answers the last page when deleting every row of the last page", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const lastPage = screen
      .getAllByRole("button", { name: /^goToPage/ })
      .at(-1)!;
    fireEvent.click(lastPage);
    await waitFor(() =>
      expect(screen.getByText(/^pageLabel.*"page":3/)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(screen.getByRole("button", { name: "bulk.delete" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );

    // Without the server's clamp this page would be empty and read "page 3 of 2".
    await waitFor(() =>
      expect(screen.getByText(/^pageLabel.*"page":2/)).toBeInTheDocument(),
    );
    expect(rowBoxes().length).toBeGreaterThan(0);
  });

  it("sends the search to the server and drops the selection when the filter changes", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(rowBoxes()[0]!);
    expect(screen.getByRole("region")).toHaveAccessibleName(
      /^selectionLabel \{"count":1\}/,
    );
    fireEvent.change(screen.getByLabelText("searchLabel"), {
      target: { value: "Coin Change" },
    });
    // The mock filters like the server does: one row left, and the old selection is gone.
    await waitFor(() => expect(rowBoxes()).toHaveLength(1));
    expect(
      screen.queryByRole("region", { name: /^selectionLabel/ }),
    ).toBeNull();
  });

  it("refuses to publish more than the server accepts in one call", async () => {
    const toasts = renderHook(() => useToasts());
    // 50 per page puts all 21 mock rows on one page.
    localStorage.setItem("algoprep-problems-page-size", "50");
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(screen.getByRole("button", { name: "bulk.publish" }));
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "warning" });
    expect(toasts.result.current.at(-1)?.message).toBe(
      'toast.publishMax {"max":20}',
    );
  });

  it("publishes the selection and reports the count", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.publish" }));
    await waitFor(() =>
      expect(toasts.result.current.at(-1)?.message).toMatch(
        /^toast\.bulk\.publish/,
      ),
    );
  });

  it("downloads the CSV of the selection", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    fireEvent.click(rowBoxes()[0]!);
    fireEvent.click(screen.getByRole("button", { name: "bulk.exportCsv" }));
    await waitFor(() => expect(saveBlob).toHaveBeenCalledTimes(1));
    expect(saveBlob.mock.calls[0]![1]).toBe("problems-mock.csv");
    expect(toasts.result.current.at(-1)?.message).toMatch(/^toast\.exported/);
  });

  it("sorts on the server when a column header is clicked", async () => {
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const firstTitle = () => rowBoxes()[0]!.getAttribute("aria-label");
    const before = firstTitle();
    // The default is "last edited, newest first"; submissions descending puts a different problem on top.
    fireEvent.click(screen.getByRole("button", { name: /columnSubmissions/ }));
    fireEvent.click(screen.getByRole("button", { name: /columnSubmissions/ }));
    await waitFor(() => expect(firstTitle()).not.toBe(before));
  });

  it("opens the authoring form pre-filled for exactly one selected problem (F2-16)", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemManagementView basePath="/admin/problems" />);
    const rows = rowBoxes();
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
    await waitFor(() => expect(rowBoxes()).toHaveLength(1));
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
    await waitFor(() => expect(rowBoxes()).toHaveLength(1));
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
    await screen.findByText("emptyFiltered");
    fireEvent.keyDown(box, { key: "Enter" });
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "info",
      message: "toast.searchEmpty",
    });
  });
});
