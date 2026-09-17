import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ConfirmDialog } from "./confirm-dialog";
import { Modal } from "./modal";

describe("Modal", () => {
  it("renders nothing while closed", () => {
    render(
      <Modal open={false} onClose={() => {}} title="Xác nhận">
        Nội dung
      </Modal>,
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("closes on Escape", () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Xác nhận">
        <button type="button">Trong hộp thoại</button>
      </Modal>,
    );

    fireEvent.keyDown(document, { key: "Escape" });

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("closes when the scrim behind the panel is clicked", () => {
    const onClose = vi.fn();
    const { container } = render(
      <Modal open onClose={onClose} title="Xác nhận">
        <button type="button">Trong hộp thoại</button>
      </Modal>,
    );

    const scrim = container.querySelector("[aria-hidden='true']");
    expect(scrim).not.toBeNull();
    fireEvent.click(scrim as Element);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("moves focus into the dialog and wraps Tab instead of letting it escape", () => {
    render(
      <Modal
        open
        onClose={() => {}}
        title="Xác nhận"
        footer={<button type="button">Đồng ý</button>}
      >
        <button type="button">Huỷ</button>
      </Modal>,
    );

    const first = screen.getByRole("button", { name: "Huỷ" });
    const last = screen.getByRole("button", { name: "Đồng ý" });
    expect(first).toHaveFocus();

    // Forward from the last element wraps to the first; jsdom does not move focus on Tab itself,
    // so this exercises the trap's own focus calls rather than the environment's.
    last.focus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(first).toHaveFocus();

    // Backward from the first wraps to the last.
    fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(last).toHaveFocus();
  });
});

describe("ConfirmDialog", () => {
  it("confirms and cancels through distinct handlers", () => {
    const onConfirm = vi.fn();
    const onClose = vi.fn();
    render(
      <ConfirmDialog
        open
        onClose={onClose}
        onConfirm={onConfirm}
        title="Khoá 3 tài khoản"
        confirmLabel="Khoá"
        cancelLabel="Huỷ"
        destructive
      >
        Hành động này chặn đăng nhập của 3 tài khoản.
      </ConfirmDialog>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Khoá" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Huỷ" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("blocks both buttons while the confirm request is in flight", () => {
    render(
      <ConfirmDialog
        open
        onClose={() => {}}
        onConfirm={() => {}}
        title="Khoá tài khoản"
        confirmLabel="Khoá"
        cancelLabel="Huỷ"
        pending
      >
        Đang xử lý
      </ConfirmDialog>,
    );

    expect(screen.getByRole("button", { name: "Khoá" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Huỷ" })).toBeDisabled();
  });
});
