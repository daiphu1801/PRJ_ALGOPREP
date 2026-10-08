import { afterEach, describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { EllipsisLink } from "./ellipsis-link";

// jsdom has no layout, so the overflow measurement is faked on the prototype.
function fakeWidths(scrollWidth: number, clientWidth: number) {
  Object.defineProperty(HTMLElement.prototype, "scrollWidth", {
    configurable: true,
    get: () => scrollWidth,
  });
  Object.defineProperty(HTMLElement.prototype, "clientWidth", {
    configurable: true,
    get: () => clientWidth,
  });
}

afterEach(() => {
  delete (HTMLElement.prototype as unknown as Record<string, unknown>)
    .scrollWidth;
  delete (HTMLElement.prototype as unknown as Record<string, unknown>)
    .clientWidth;
});

describe("EllipsisLink", () => {
  it("makes the link only as wide as its text, truncating with an ellipsis", () => {
    render(
      <EllipsisLink href="/x">Best Time to Buy and Sell Stock</EllipsisLink>,
    );
    const link = screen.getByRole("link", {
      name: "Best Time to Buy and Sell Stock",
    });
    // A flex item (min-w-0 so it can shrink), not a block filling the cell.
    expect(link).toHaveClass("min-w-0", "truncate");
    expect(link.parentElement).toHaveClass("flex");
  });

  it("shows the full text as a tooltip only when the text is actually cut", () => {
    fakeWidths(300, 120);
    render(
      <EllipsisLink href="/x">
        A very long title that does not fit its column
      </EllipsisLink>,
    );
    const link = screen.getByRole("link");
    expect(link).not.toHaveAttribute("title");
    fireEvent.mouseEnter(link);
    expect(link).toHaveAttribute(
      "title",
      "A very long title that does not fit its column",
    );
  });

  it("adds no tooltip when the text fits", () => {
    fakeWidths(100, 100);
    render(<EllipsisLink href="/x">Short</EllipsisLink>);
    const link = screen.getByRole("link");
    fireEvent.mouseEnter(link);
    expect(link).not.toHaveAttribute("title");
  });
});
