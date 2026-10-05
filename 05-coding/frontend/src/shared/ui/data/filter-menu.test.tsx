import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { FilterMenu } from "./filter-menu";

const options = [
  { value: "all", label: "All" },
  { value: "easy", label: "Easy" },
  { value: "hard", label: "Hard" },
];

function setup(onValueChange = vi.fn(), value = "all") {
  render(<FilterMenu label="By level" options={options} value={value} onValueChange={onValueChange} />);
  return screen.getByRole("button", { name: /^By level/ });
}

describe("FilterMenu", () => {
  it("shows the current choice on the trigger and keeps the options hidden until opened", () => {
    const trigger = setup(vi.fn(), "easy");
    expect(trigger).toHaveTextContent("By level: Easy");
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("listbox")).toBeNull();
  });

  it("opens a listbox, marks the current option, and reports a pick then closes", () => {
    const onValueChange = vi.fn();
    const trigger = setup(onValueChange);
    fireEvent.click(trigger);
    expect(screen.getByRole("option", { name: "All" })).toHaveAttribute("aria-selected", "true");
    fireEvent.click(screen.getByRole("option", { name: "Hard" }));
    expect(onValueChange).toHaveBeenCalledWith("hard");
    expect(screen.queryByRole("listbox")).toBeNull();
    expect(trigger).toHaveFocus();
  });

  it("moves with the arrow keys and closes on Escape, returning focus to the trigger", () => {
    const trigger = setup();
    fireEvent.click(trigger);
    const first = screen.getByRole("option", { name: "All" });
    expect(first).toHaveFocus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(screen.getByRole("option", { name: "Easy" })).toHaveFocus();
    fireEvent.keyDown(screen.getByRole("option", { name: "Easy" }), { key: "Escape" });
    expect(screen.queryByRole("listbox")).toBeNull();
    expect(trigger).toHaveFocus();
  });

  it("closes on a click outside", () => {
    fireEvent.click(setup());
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole("listbox")).toBeNull();
  });
});
