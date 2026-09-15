// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HalfDonutGauge } from "./half-donut-gauge";

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
});
