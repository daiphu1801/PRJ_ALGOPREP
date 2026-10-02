import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
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

  it("shows the error the store reports when adding", () => {
    setup(vi.fn(), () => "duplicate");
    fireEvent.change(screen.getByLabelText("Chủ đề mới"), { target: { value: "Alpha" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Trùng");
  });
});
