import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Badge, type BadgeVariant } from "./badge";

const VARIANTS: BadgeVariant[] = ["neutral", "cyan", "teal", "success", "warn", "negative"];

describe("Badge", () => {
  it.each(VARIANTS)("renders the %s variant with a pill shape", (variant) => {
    render(<Badge variant={variant}>Đã xuất bản</Badge>);
    const el = screen.getByText("Đã xuất bản");
    expect(el.className).toContain("rounded-full");
  });

  it("maps each variant to a distinct class string", () => {
    const seen = new Set<string>();
    for (const variant of VARIANTS) {
      const { unmount } = render(<Badge variant={variant}>x</Badge>);
      seen.add(screen.getByText("x").className);
      unmount();
    }
    expect(seen.size).toBe(VARIANTS.length);
  });
});
