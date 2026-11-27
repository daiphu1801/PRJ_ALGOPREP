import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Avatar } from "./avatar";

describe("Avatar", () => {
  it("draws the initials with the full name as accessible name when there is no src", () => {
    render(<Avatar name="Nguyễn Văn An" />);
    const root = screen.getByRole("img", { name: "Nguyễn Văn An" });
    expect(root).toHaveTextContent("VA");
    expect(root.querySelector("img")).toBeNull();
  });

  it("draws the image when src is set", () => {
    const { container } = render(<Avatar name="An" src="blob:x" />);
    expect(container.querySelector("img")).toHaveAttribute("src", "blob:x");
  });

  it("falls back to the initials when the image fails and retries on a new src", () => {
    const { container, rerender } = render(
      <Avatar name="Lê Minh" src="blob:a" />,
    );
    fireEvent.error(container.querySelector("img") as HTMLImageElement);
    expect(container.querySelector("img")).toBeNull();
    expect(screen.getByRole("img", { name: "Lê Minh" })).toHaveTextContent(
      "LM",
    );
    rerender(<Avatar name="Lê Minh" src="blob:b" />);
    expect(container.querySelector("img")).toHaveAttribute("src", "blob:b");
  });

  it("shows a question mark for an empty name", () => {
    render(<Avatar name="" />);
    expect(screen.getByRole("img")).toHaveTextContent("?");
  });
});
