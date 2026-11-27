import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ColumnChart } from "./column-chart";

describe("ColumnChart", () => {
  it("shows each label with its value or custom value label", () => {
    render(
      <ColumnChart
        items={[
          { label: "Lớp A", value: 81, valueLabel: "81%" },
          { label: "Lớp B", value: 4 },
        ]}
      />,
    );
    expect(screen.getByText("Lớp A")).toBeInTheDocument();
    expect(screen.getByText("81%")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("scales bars against max, or against the largest value when max is omitted", () => {
    const { container, rerender } = render(
      <ColumnChart items={[{ label: "A", value: 50 }]} max={100} />,
    );
    const bar = () => container.querySelector("li > div > div") as HTMLElement;
    expect(bar().style.height).toBe("50%");
    rerender(
      <ColumnChart
        items={[
          { label: "A", value: 50 },
          { label: "B", value: 200 },
        ]}
      />,
    );
    expect(bar().style.height).toBe("25%");
  });
});
