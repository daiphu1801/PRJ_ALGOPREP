import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { BulkActionBar } from "./bulk-action-bar";

describe("BulkActionBar", () => {
  it("renders nothing visible at zero, but keeps the status region mounted", () => {
    render(
      <BulkActionBar count={0} label="Selected 0">
        <button type="button">Go</button>
      </BulkActionBar>,
    );
    expect(screen.queryByRole("button", { name: "Go" })).toBeNull();
    expect(screen.getByRole("status")).toBeEmptyDOMElement();
  });

  it("shows the label, the actions and announces the count", () => {
    render(
      <BulkActionBar count={2} label="Selected 2">
        <button type="button">Go</button>
      </BulkActionBar>,
    );
    expect(screen.getByRole("button", { name: "Go" })).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Selected 2");
  });

  it("floats out of the surrounding card, into the nearest shell root", () => {
    const { container } = render(
      <div className="admin-shell" data-testid="shell">
        <section className="glass-card" data-testid="card">
          <BulkActionBar count={1} label="Selected 1">
            <button type="button">Go</button>
          </BulkActionBar>
        </section>
      </div>,
    );
    const bar = screen.getByRole("region", { name: "Selected 1" });
    expect(bar.parentElement).toBe(container.querySelector(".admin-shell"));
    expect(screen.getByTestId("card")).not.toContainElement(bar);
  });
});
