import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Pagination } from "./pagination";

const base = {
  page: 1,
  pageSize: 20,
  onPageChange: () => {},
  summary: "Trang 1 / 1",
  previousLabel: "Trước",
  nextLabel: "Sau",
};

describe("Pagination", () => {
  it("shows the stepper by default even for a single page", () => {
    render(<Pagination {...base} total={5} />);
    expect(screen.getByText("Trang 1 / 1")).toBeInTheDocument();
  });

  it("hides itself on a single page only when asked to", () => {
    const { container, rerender } = render(
      <Pagination {...base} total={20} hideWhenSinglePage />,
    );
    expect(container).toBeEmptyDOMElement();
    rerender(<Pagination {...base} total={21} hideWhenSinglePage />);
    expect(screen.getByText("Trang 1 / 1")).toBeInTheDocument();
  });
});
