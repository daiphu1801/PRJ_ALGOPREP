import { afterEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import type { ManagedListError } from "@/shared/lib/managed-list-store";
import { ManagedListDialog, type ManagedListLabels } from "./managed-list-dialog";

const labels: ManagedListLabels = {
  title: "Quản lý chủ đề",
  hint: "hint",
  nameLabel: "Tên",
  newLabel: "Chủ đề mới",
  newPlaceholder: "",
  add: "Thêm",
  save: "Lưu tên",
  delete: "Xoá",
  close: "Đóng",
  usage: (count) => `${count} câu`,
  deleteBlocked: (count) => `Còn ${count} câu`,
  error: { empty: "Trống", duplicate: "Trùng" },
  done: { add: "Đã thêm", rename: "Đã đổi tên", remove: "Đã xoá" },
};

const items = [
  { key: "a", label: "Alpha" },
  { key: "b", label: "Beta" },
];

function setup(
  onRemove = vi.fn(),
  onAdd: (label: string) => ManagedListError | null = () => null,
  options: { items?: typeof items; keepAtLeast?: number; onMove?: (key: string, delta: -1 | 1) => void } = {},
) {
  render(
    <ManagedListDialog
      open
      onClose={() => {}}
      items={options.items ?? items}
      usage={{ a: 3 }}
      keepAtLeast={options.keepAtLeast}
      onMove={options.onMove}
      labels={{ ...labels, deleteLast: "Phải còn một mục", moveUp: "Lên", moveDown: "Xuống" }}
      onAdd={onAdd}
      onRename={() => null}
      onRemove={onRemove}
    />,
  );
  return { onRemove, onAdd };
}

afterEach(() => act(() => toast.clear()));

describe("ManagedListDialog", () => {
  it("refuses deleting an item that is still in use and explains why", () => {
    const { onRemove } = setup();
    const blocked = screen.getByRole("button", { name: "Còn 3 câu" });
    fireEvent.click(blocked);
    expect(onRemove).not.toHaveBeenCalled();
    expect(blocked).toHaveAttribute("aria-disabled", "true");
  });

  it("deletes an unused item", () => {
    const { onRemove } = setup();
    fireEvent.click(screen.getByRole("button", { name: "Xoá" }));
    expect(onRemove).toHaveBeenCalledWith("b");
  });

  it("raises an error toast and flags the field when the store rejects an add", () => {
    const toasts = renderHook(() => useToasts());
    setup(vi.fn(), () => "duplicate");
    fireEvent.change(screen.getByLabelText("Chủ đề mới"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm" }));
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([["error", "Trùng"]]);
    expect(screen.getByLabelText("Chủ đề mới")).toHaveAttribute("aria-invalid", "true");
    act(() => toast.clear());
  });

  it("raises a success toast after a delete", () => {
    const toasts = renderHook(() => useToasts());
    setup();
    fireEvent.click(screen.getByRole("button", { name: "Xoá" }));
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([["success", "Đã xoá"]]);
    act(() => toast.clear());
  });

  it("disables delete once the list is down to keepAtLeast and says why", () => {
    const { onRemove } = setup(vi.fn(), () => null, { items: [items[1]!], keepAtLeast: 1 });
    const last = screen.getByRole("button", { name: "Phải còn một mục" });
    fireEvent.click(last);
    expect(onRemove).not.toHaveBeenCalled();
    expect(last).toHaveAttribute("aria-disabled", "true");
  });

  it("still deletes when the list is above keepAtLeast", () => {
    const { onRemove } = setup(vi.fn(), () => null, { keepAtLeast: 1 });
    fireEvent.click(screen.getByRole("button", { name: "Xoá" }));
    expect(onRemove).toHaveBeenCalledWith("b");
  });

  it("shows move controls only when onMove is given, disabled at the ends", () => {
    const onMove = vi.fn();
    setup(vi.fn(), () => null, { onMove });
    const ups = screen.getAllByRole("button", { name: "Lên" });
    const downs = screen.getAllByRole("button", { name: "Xuống" });
    expect(ups[0]).toHaveAttribute("aria-disabled", "true");
    expect(downs[1]).toHaveAttribute("aria-disabled", "true");
    fireEvent.click(downs[0]!);
    expect(onMove).toHaveBeenCalledWith("a", 1);
    fireEvent.click(ups[1]!);
    expect(onMove).toHaveBeenCalledWith("b", -1);
  });

  it("renders no move controls without onMove", () => {
    setup();
    expect(screen.queryByRole("button", { name: "Lên" })).not.toBeInTheDocument();
  });
});

describe("ManagedListDialog long lists", () => {
  const many = (count: number) =>
    Array.from({ length: count }, (_, index) => ({ key: `k${index}`, label: `Item ${index}` }));

  it("grows freely up to 5 items", () => {
    setup(vi.fn(), () => null, { items: many(5) });
    expect(screen.getByRole("list")).not.toHaveClass("overflow-y-auto");
  });

  it("scrolls from 6 items on, with a height cap", () => {
    setup(vi.fn(), () => null, { items: many(6) });
    const list = screen.getByRole("list");
    expect(list).toHaveClass("overflow-y-auto");
    expect(list.style.maxHeight).toBe("14.5rem");
  });
});
