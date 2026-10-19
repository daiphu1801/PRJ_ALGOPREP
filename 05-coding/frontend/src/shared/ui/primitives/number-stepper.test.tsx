import { act, fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { NumberStepper } from "./number-stepper";

function Harness({
  editable,
  onChange,
}: {
  editable?: boolean;
  onChange?: (n: number) => void;
}) {
  const [value, setValue] = useState(10);
  return (
    <NumberStepper
      value={value}
      onValueChange={(next) => {
        setValue(next);
        onChange?.(next);
      }}
      label="Limit"
      min={1}
      max={50}
      step={5}
      format={(n) => `${n} per hour`}
      decrementLabel="less"
      incrementLabel="more"
      editable={editable}
    />
  );
}

describe("NumberStepper", () => {
  it("stays a plain readout by default, so existing screens do not change", () => {
    render(<Harness />);
    expect(screen.getByText("10 per hour")).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.getByRole("spinbutton", { name: "Limit" })).toHaveAttribute(
      "aria-valuenow",
      "10",
    );
  });

  it("editable: only the number is in the box, the unit stays outside it, and a typed number commits on blur", () => {
    const onChange = vi.fn();
    render(<Harness editable onChange={onChange} />);
    const box = screen.getByRole("spinbutton", { name: "Limit" });
    expect(box).toHaveValue("10");
    expect(screen.getByText("per hour")).toBeInTheDocument();

    act(() => box.focus());
    fireEvent.change(box, { target: { value: "37" } });
    act(() => box.blur());
    expect(onChange).toHaveBeenCalledWith(37);
    expect(box).toHaveValue("37");
    expect(screen.getByRole("spinbutton", { name: "Limit" })).toHaveAttribute(
      "aria-valuetext",
      "37 per hour",
    );
  });

  it("editable: pulls an out-of-range number to the nearest bound and drops text that is not a whole number", () => {
    const onChange = vi.fn();
    render(<Harness editable onChange={onChange} />);
    const box = screen.getByRole("spinbutton", { name: "Limit" });

    act(() => box.focus());
    fireEvent.change(box, { target: { value: "999" } });
    act(() => box.blur());
    expect(onChange).toHaveBeenLastCalledWith(50);

    act(() => box.focus());
    fireEvent.change(box, { target: { value: "abc" } });
    act(() => box.blur());
    expect(box).toHaveValue("50");
    act(() => box.focus());
    fireEvent.change(box, { target: { value: "2.5" } });
    act(() => box.blur());
    expect(box).toHaveValue("50");
    // A trailing unit is tolerated: "30 per hour" or "30%" both mean 30.
    act(() => box.focus());
    fireEvent.change(box, { target: { value: "30 per hour" } });
    act(() => box.blur());
    expect(onChange).toHaveBeenLastCalledWith(30);
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it("editable: Escape reverts, Enter commits, arrows step, and the buttons still work", () => {
    const onChange = vi.fn();
    render(<Harness editable onChange={onChange} />);
    const box = screen.getByRole("spinbutton", { name: "Limit" });

    act(() => box.focus());
    fireEvent.change(box, { target: { value: "20" } });
    fireEvent.keyDown(box, { key: "Escape" });
    expect(onChange).not.toHaveBeenCalled();

    act(() => box.focus());
    fireEvent.change(box, { target: { value: "20" } });
    fireEvent.keyDown(box, { key: "Enter" });
    expect(onChange).toHaveBeenLastCalledWith(20);

    act(() => box.focus());
    fireEvent.keyDown(box, { key: "ArrowUp" });
    expect(onChange).toHaveBeenLastCalledWith(25);

    fireEvent.click(screen.getByRole("button", { name: "less" }));
    expect(onChange).toHaveBeenLastCalledWith(20);
  });
});
