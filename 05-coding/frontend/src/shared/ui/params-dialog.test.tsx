import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ParamsDialog } from "./params-dialog";

const labels = { save: "Lưu", cancel: "Huỷ", errorMin: (min: number) => `Từ ${min} trở lên` };
const fields = [{ type: "number", key: "days", label: "Thời hạn", min: 1 }] as const;

describe("ParamsDialog", () => {
  it("saves a valid whole number and closes", () => {
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(
      <ParamsDialog open onClose={onClose} title="T" fields={fields} values={{ days: "90" }} onSave={onSave} labels={labels} />,
    );
    fireEvent.change(screen.getByLabelText("Thời hạn"), { target: { value: "120" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onSave).toHaveBeenCalledWith({ days: "120" });
    expect(onClose).toHaveBeenCalled();
  });

  it("blocks saving below the minimum and shows the error", () => {
    const onSave = vi.fn();
    render(
      <ParamsDialog open onClose={() => {}} title="T" fields={fields} values={{ days: "90" }} onSave={onSave} labels={labels} />,
    );
    fireEvent.change(screen.getByLabelText("Thời hạn"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent("Từ 1 trở lên");
  });
});
