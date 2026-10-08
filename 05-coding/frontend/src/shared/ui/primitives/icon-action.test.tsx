import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Pencil } from "lucide-react";
import { IconAction } from "./icon-action";

describe("IconAction", () => {
  it("names the button by ariaLabel but shows the short label as the tooltip", () => {
    render(
      <IconAction icon={Pencil} label="Sửa" ariaLabel="Sửa bài Word Break" />,
    );
    const button = screen.getByRole("button", { name: "Sửa bài Word Break" });
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.mouseEnter(button);
    expect(screen.getByRole("tooltip")).toHaveTextContent("Sửa");
    fireEvent.mouseLeave(button);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
  });

  it("falls back to label as the accessible name", () => {
    render(<IconAction icon={Pencil} label="Sửa" />);
    expect(screen.getByRole("button", { name: "Sửa" })).toBeInTheDocument();
  });

  it("a disabled action ignores clicks but still shows its tooltip", () => {
    let clicks = 0;
    render(
      <IconAction
        icon={Pencil}
        label="Còn 3 câu"
        disabled
        onClick={() => (clicks += 1)}
      />,
    );
    const button = screen.getByRole("button", { name: "Còn 3 câu" });
    fireEvent.click(button);
    expect(clicks).toBe(0);
    expect(button).toHaveAttribute("aria-disabled", "true");
    fireEvent.mouseEnter(button);
    expect(screen.getByRole("tooltip")).toHaveTextContent("Còn 3 câu");
  });
});
