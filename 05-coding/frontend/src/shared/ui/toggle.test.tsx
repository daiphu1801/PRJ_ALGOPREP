import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Toggle } from "./toggle";

describe("Toggle", () => {
  it("exposes switch semantics, not checkbox semantics", () => {
    render(<Toggle checked onCheckedChange={() => {}} label="Bật ngôn ngữ Python" />);
    const el = screen.getByRole("switch", { name: "Bật ngôn ngữ Python" });
    expect(el).toHaveAttribute("aria-checked", "true");
  });

  it("is a real <button>, so the browser supplies Enter/Space and focus for free", () => {
    render(<Toggle checked={false} onCheckedChange={() => {}} label="Bật C++" />);
    const el = screen.getByRole("switch");
    expect(el.tagName).toBe("BUTTON");
    el.focus();
    expect(el).toHaveFocus();
  });

  it("reports the flipped value, not the current one", () => {
    const onCheckedChange = vi.fn();
    render(<Toggle checked={false} onCheckedChange={onCheckedChange} label="Bật C++" />);

    fireEvent.click(screen.getByRole("switch"));

    expect(onCheckedChange).toHaveBeenCalledWith(true);
  });

  it("blocks input while a server write is pending", () => {
    const onCheckedChange = vi.fn();
    render(<Toggle checked={false} onCheckedChange={onCheckedChange} label="Bật Java" pending />);

    fireEvent.click(screen.getByRole("switch"));

    expect(onCheckedChange).not.toHaveBeenCalled();
    expect(screen.getByRole("switch")).toHaveAttribute("aria-busy", "true");
  });

  it("labels via the visible text node when showLabel is set", () => {
    render(<Toggle checked onCheckedChange={() => {}} label="Cho phép mạng" showLabel />);
    // A <label> cannot label a <button>, so this must resolve through aria-labelledby.
    expect(screen.getByRole("switch", { name: "Cho phép mạng" })).toBeInTheDocument();
  });
});
