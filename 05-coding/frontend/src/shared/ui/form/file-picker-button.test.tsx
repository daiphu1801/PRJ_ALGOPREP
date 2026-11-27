import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { FilePickerButton } from "./file-picker-button";

describe("FilePickerButton", () => {
  it("hands every picked file to onFiles, and passes accept and multiple on", () => {
    const onFiles = vi.fn();
    render(
      <FilePickerButton
        accept=".in,.out"
        multiple
        inputTestId="pick"
        onFiles={onFiles}
      >
        Choose
      </FilePickerButton>,
    );
    const input = screen.getByTestId("pick");
    expect(input).toHaveAttribute("accept", ".in,.out");
    expect(input).toHaveAttribute("multiple");
    const a = new File(["a"], "a.in");
    const b = new File(["b"], "a.out");
    fireEvent.change(input, { target: { files: [a, b] } });
    expect(onFiles).toHaveBeenCalledWith([a, b]);
  });

  it("opens the chooser from the button and ignores an empty pick", () => {
    const onFiles = vi.fn();
    render(
      <FilePickerButton inputTestId="pick" onFiles={onFiles}>
        Choose
      </FilePickerButton>,
    );
    const click = vi
      .spyOn(screen.getByTestId("pick"), "click")
      .mockImplementation(() => undefined);
    fireEvent.click(screen.getByRole("button", { name: "Choose" }));
    expect(click).toHaveBeenCalledTimes(1);
    fireEvent.change(screen.getByTestId("pick"), { target: { files: [] } });
    expect(onFiles).not.toHaveBeenCalled();
  });

  it("is disabled with the button's own props", () => {
    render(
      <FilePickerButton disabled onFiles={vi.fn()}>
        Choose
      </FilePickerButton>,
    );
    expect(screen.getByRole("button", { name: "Choose" })).toBeDisabled();
  });
});
