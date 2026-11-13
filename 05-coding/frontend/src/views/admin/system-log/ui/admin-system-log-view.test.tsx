// Mock-mode behaviour of ADM0403 (the mock honours the same filters and cursor as the server): "Tải
// thêm" loads the next page, the day range filters it, and a lock row shows its reason while an unlock
// row does not. The real-API path is in admin-system-log-real.test.tsx.
import type { ReactElement } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ExportLogDialog } from "./export-log-dialog";
import { AdminSystemLogView } from "./admin-system-log-view";

// The list loads through a query, so each render waits for the first page.
async function renderView(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findByRole("list", { name: "eventListLabel" });
}

vi.mock("@/shared/i18n", () => ({
  useApiErrorMessage: () => () => "apiError",
  useLocale: () => "vi",
  useT: () => (key: string, values?: Record<string, string | number>) =>
    values ? `${key}:${Object.values(values).join(",")}` : key,
}));

const rows = () =>
  screen
    .getAllByRole("listitem")
    .filter((item) => item.className.includes("border-l-2"));

describe("AdminSystemLogView", () => {
  it("shows the lock reason on the lock row only", async () => {
    await renderView(<AdminSystemLogView />);
    // The first page holds the ten seed rows and their synthetic repeats: every lock row has the
    // reason, no unlock row does.
    const locks = screen.getAllByText("Khóa tài khoản hmtri");
    expect(locks.length).toBeGreaterThan(0);
    expect(screen.getAllByText(/^reasonLabel: /)).toHaveLength(locks.length);
    expect(
      screen.getAllByText("Mở khóa tài khoản bklinh").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText(/Phát hiện nhiều tài khoản/)).toHaveLength(
      locks.length,
    );
  });

  it("loads 50 more rows from the load-more button", async () => {
    await renderView(<AdminSystemLogView />);
    const before = rows().length;
    fireEvent.click(screen.getByRole("button", { name: "loadMore:50" }));
    await waitFor(() => expect(rows()).toHaveLength(before + 50));
  });

  it("filters by day range, inclusive on both ends", async () => {
    await renderView(<AdminSystemLogView />);
    fireEvent.click(screen.getByRole("button", { name: "loadMore:50" }));

    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-08" },
    });
    fireEvent.change(screen.getByLabelText("dateTo"), {
      target: { value: "2026-10-08" },
    });

    // Today's seed rows only: the synthetic history starts yesterday.
    await waitFor(() => expect(rows()).toHaveLength(10));
  });

  it("returns nothing, and says why, when the range is reversed", async () => {
    await renderView(<AdminSystemLogView />);
    fireEvent.change(screen.getByLabelText("dateTo"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-05" },
    });
    expect(screen.getByLabelText("dateTo")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByText("emptyFiltered")).toBeInTheDocument();
  });

  it("export dialog carries the filters on screen and refuses future and too-old dates", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ExportLogDialog
          open
          onClose={() => {}}
          retentionDays={90}
          scope={{
            query: "pdai",
            category: "auth",
            categoryLabel: "category.auth",
            from: "",
            to: "",
          }}
        />
      </QueryClientProvider>,
    );
    expect(screen.getByText(/exportDialog.scopeTitle/)).toHaveTextContent(
      "exportDialog.scopeQuery:pdai",
    );

    const day = (offset: number) =>
      new Date(Date.now() + offset * 86_400_000).toISOString().slice(0, 10);
    fireEvent.change(screen.getByLabelText("exportDialog.from"), {
      target: { value: day(-400) },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "exportDialog.submit" }),
    );
    expect(toasts.result.current.at(-1)?.message).toBe(
      "exportDialog.errorRetention:90",
    );

    fireEvent.change(screen.getByLabelText("exportDialog.from"), {
      target: { value: day(-1) },
    });
    fireEvent.change(screen.getByLabelText("exportDialog.to"), {
      target: { value: day(3) },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "exportDialog.submit" }),
    );
    expect(toasts.result.current.at(-1)?.message).toBe(
      "exportDialog.errorFuture",
    );
  });
});
