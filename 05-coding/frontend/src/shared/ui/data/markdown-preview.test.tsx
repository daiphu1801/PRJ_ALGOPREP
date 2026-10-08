import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MarkdownPreview } from "./markdown-preview";

describe("MarkdownPreview", () => {
  it("renders headings, lists and inline code instead of the raw Markdown", () => {
    render(
      <MarkdownPreview>
        {"## Đề bài\n\n- một\n- hai\n\nDùng `s` và `t`."}
      </MarkdownPreview>,
    );

    expect(
      screen.getByRole("heading", { level: 2, name: "Đề bài" }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.queryByText(/##/)).not.toBeInTheDocument();
  });

  it("renders LaTeX through KaTeX", () => {
    const { container } = render(
      <MarkdownPreview>{"Độ phức tạp $O(n^2)$"}</MarkdownPreview>,
    );

    expect(container.querySelector(".katex")).not.toBeNull();
  });

  it("does not turn raw HTML into elements", () => {
    const { container } = render(
      <MarkdownPreview>
        {'<img src=x onerror="alert(1)"> an toàn'}
      </MarkdownPreview>,
    );

    expect(container.querySelector("img")).toBeNull();
  });
});
