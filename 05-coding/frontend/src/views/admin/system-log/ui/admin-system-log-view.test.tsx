// The three things ADM0403 added on 2026-10-08: "Tải thêm" loads rows, the day range filters them,
// and a lock row shows its reason while an unlock row does not.
import { describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ExportLogDialog } from "./export-log-dialog";
import { AdminSystemLogView } from "./admin-system-log-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, values?: Record<string, string | number>) =>
    values ? `${key}:${Object.values(values).join(",")}` : key,
}));

const rows = () =>
  screen
    .getAllByRole("listitem")
    .filter((item) => item.className.includes("border-l-2"));

describe("AdminSystemLogView", () => {
  it("shows the lock reason on the lock row only", () => {
    render(<AdminSystemLogView />);
    expect(screen.getAllByText(/^reasonLabel: /)).toHaveLength(1);
    expect(screen.getByText(/Phát hiện nhiều tài khoản/)).toBeInTheDocument();
  });

  it("loads 50 more rows from the load-more button", () => {
    render(<AdminSystemLogView />);
    const before = rows().length;
    fireEvent.click(screen.getByRole("button", { name: "loadMore:50" }));
    expect(rows()).toHaveLength(before + 50);
  });

  it("filters by day range, inclusive on both ends", () => {
    render(<AdminSystemLogView />);
    fireEvent.click(screen.getByRole("button", { name: "loadMore:50" }));

    fireEvent.change(screen.getByLabelText("dateFrom"), {
      target: { value: "2026-10-08" },
    });
    fireEvent.change(screen.getByLabelText("dateTo"), {
      target: { value: "2026-10-08" },
    });

    // Today's seed rows only: the synthetic history starts yesterday.
    expect(rows()).toHaveLength(10);
  });

  it("returns nothing, and says why, when the range is reversed", () => {
    render(<AdminSystemLogView />);
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

  it("export dialog carries the filters on screen and refuses future and too-old dates", () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(
      <ExportLogDialog
        open
        onClose={() => {}}
        scope={{
          query: "pdai",
          categoryLabel: "category.auth",
          from: "",
          to: "",
        }}
      />,
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
