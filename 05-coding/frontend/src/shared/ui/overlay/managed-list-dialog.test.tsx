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
) {
  render(
    <ManagedListDialog
      open
      onClose={() => {}}
      items={items}
      usage={{ a: 3 }}
      labels={labels}
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
});
