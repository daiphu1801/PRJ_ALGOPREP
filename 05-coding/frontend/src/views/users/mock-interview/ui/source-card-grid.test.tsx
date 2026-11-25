import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { FileCode2 } from "lucide-react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { SourceCardGrid, type SourceCardData } from "./source-card-grid";

const cards = (n: number): SourceCardData[] =>
  Array.from({ length: n }, (_, i) => ({
    id: `c${i + 1}`,
    title: `Bài ${i + 1}`,
    caption: `Nộp ${i + 1}/11/2026`,
  }));

function renderGrid(n: number, selectedId: string | null = null) {
  const onPick = vi.fn();
  render(
    withTestProviders(
      <SourceCardGrid
        icon={FileCode2}
        cards={cards(n)}
        selectedId={selectedId}
        onPick={onPick}
      />,
    ),
  );
  return onPick;
}

describe("SourceCardGrid", () => {
  it("shows every card and no toggle when there are six or fewer", () => {
    renderGrid(6);
    expect(screen.getAllByRole("button", { name: /Bài \d/ })).toHaveLength(6);
    expect(screen.queryByRole("button", { name: /Xem thêm/ })).toBeNull();
  });

  it("keeps the first six and opens the rest behind 'Xem thêm'", () => {
    renderGrid(9);
    expect(screen.getAllByRole("button", { name: /Bài \d/ })).toHaveLength(6);
    fireEvent.click(screen.getByRole("button", { name: "Xem thêm 3 bài" }));
    expect(screen.getAllByRole("button", { name: /Bài \d/ })).toHaveLength(9);
    fireEvent.click(screen.getByRole("button", { name: "Thu gọn" }));
    expect(screen.getAllByRole("button", { name: /Bài \d/ })).toHaveLength(6);
  });

  it("never hides the chosen card behind the toggle", () => {
    renderGrid(9, "c8");
    expect(screen.getAllByRole("button", { name: /Bài \d/ })).toHaveLength(9);
    expect(screen.getByRole("button", { name: /Bài 8/ })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
  });

  it("reports the picked id and shows the caption that tells two submissions apart", () => {
    const onPick = renderGrid(2);
    expect(screen.getByText("Nộp 2/11/2026")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Bài 2/ }));
    expect(onPick).toHaveBeenCalledWith("c2");
  });
});
