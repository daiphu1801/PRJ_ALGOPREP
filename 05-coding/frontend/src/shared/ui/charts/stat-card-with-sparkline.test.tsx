import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatCardWithSparkline } from "./stat-card-with-sparkline";

const base = { label: "Active", value: "42", sparklineSeries: [1, 2, 3] };

describe("StatCardWithSparkline", () => {
  it("shows a signed percent for up and down", () => {
    const { rerender } = render(
      <StatCardWithSparkline
        {...base}
        deltaPercent={12.5}
        deltaDirection="up"
      />,
    );
    expect(screen.getByText("+12,5%")).toBeInTheDocument();

    rerender(
      <StatCardWithSparkline
        {...base}
        deltaPercent={3}
        deltaDirection="down"
      />,
    );
    expect(screen.getByText("-3,0%")).toBeInTheDocument();
  });

  it("shows a dash instead of a percent when the period before was zero", () => {
    render(
      <StatCardWithSparkline
        {...base}
        deltaPercent={null}
        deltaDirection="none"
      />,
    );
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });
});
