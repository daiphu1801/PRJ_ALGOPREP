// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HalfDonutGauge, wholePercents } from "./half-donut-gauge";

describe("HalfDonutGauge", () => {
  it("renders one legend entry per slice with percentages that sum to 100", () => {
    render(
      <HalfDonutGauge
        slices={[
          { label: "AC", value: 60, colorVar: "--color-primary" },
          { label: "WA", value: 40, colorVar: "--color-danger" },
        ]}
      />,
    );
    expect(screen.getByText(/AC — 60%/)).toBeInTheDocument();
    expect(screen.getByText(/WA — 40%/)).toBeInTheDocument();
  });

  it("wholePercents always sums to 100, giving leftover points to the largest remainder", () => {
    // Rounding each third alone gives 33 + 33 + 33 = 99.
    expect(wholePercents([1, 1, 1]).reduce((a, b) => a + b, 0)).toBe(100);
    // 2/3 = 66.67, 1/6 = 16.67 each: rounding alone gives 67 + 17 + 17 = 101.
    expect(wholePercents([4, 1, 1])).toEqual([66, 17, 17]);
    expect(wholePercents([0, 0])).toEqual([0, 0]);
  });
});
