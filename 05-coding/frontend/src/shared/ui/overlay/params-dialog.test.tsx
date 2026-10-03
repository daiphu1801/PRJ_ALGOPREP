import { describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen } from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ParamsDialog } from "./params-dialog";

const labels = { save: "Lưu", cancel: "Huỷ", saved: "Đã lưu", errorMin: (min: number) => `Từ ${min} trở lên` };
const fields = [{ type: "number", key: "days", label: "Thời hạn", min: 1 }] as const;

describe("ParamsDialog", () => {
  it("saves a valid whole number, toasts and closes", () => {
    const toasts = renderHook(() => useToasts());
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(
      <ParamsDialog open onClose={onClose} title="T" fields={fields} values={{ days: "90" }} onSave={onSave} labels={labels} />,
    );
    fireEvent.change(screen.getByLabelText("Thời hạn"), { target: { value: "120" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onSave).toHaveBeenCalledWith({ days: "120" });
    expect(onClose).toHaveBeenCalled();
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([["success", "Đã lưu"]]);
    act(() => toast.clear());
  });

  it("blocks saving below the minimum, flags the field and toasts the error", () => {
    const toasts = renderHook(() => useToasts());
    const onSave = vi.fn();
    render(
      <ParamsDialog open onClose={() => {}} title="T" fields={fields} values={{ days: "90" }} onSave={onSave} labels={labels} />,
    );
    fireEvent.change(screen.getByLabelText("Thời hạn"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByLabelText("Thời hạn")).toHaveAttribute("aria-invalid", "true");
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([["error", "Từ 1 trở lên"]]);
    act(() => toast.clear());
  });
});
