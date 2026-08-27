import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "./button";

describe("Button", () => {
  it("render children và nhận className tuỳ biến", () => {
    render(<Button className="extra">Nộp bài</Button>);
    const el = screen.getByRole("button", { name: "Nộp bài" });
    expect(el).toBeInTheDocument();
    expect(el.className).toContain("extra");
  });

  it("asChild truyền style xuống thẻ con thay vì render <button>", () => {
    render(
      <Button asChild>
        <a href="/login">Đăng nhập</a>
      </Button>,
    );
    // Phải là link, KHÔNG phải button — nếu hỏng thì mọi chỗ dùng link-dạng-nút sẽ mất a11y.
    const link = screen.getByRole("link", { name: "Đăng nhập" });
    expect(link).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(link.className).toContain("rounded-md");
  });
});
