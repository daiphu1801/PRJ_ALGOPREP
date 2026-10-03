import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "./button";

describe("Button", () => {
  it("renders children and accepts a custom className", () => {
    render(<Button className="extra">Nộp bài</Button>);
    const el = screen.getByRole("button", { name: "Nộp bài" });
    expect(el).toBeInTheDocument();
    expect(el.className).toContain("extra");
  });

  it("asChild forwards styling to the child element instead of rendering a <button>", () => {
    render(
      <Button asChild>
        <a href="/login">Đăng nhập</a>
      </Button>,
    );
    // Must be a link, NOT a button — if this breaks, every link-styled-as-button loses a11y.
    const link = screen.getByRole("link", { name: "Đăng nhập" });
    expect(link).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(link.className).toContain("rounded-md");
  });
});
