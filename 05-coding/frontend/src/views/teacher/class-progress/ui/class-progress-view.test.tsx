// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassProgressView } from "./class-progress-view";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

describe("ClassProgressView", () => {
  // "Cần chú ý" was removed 2026-09-27; a student's standing is now the table's own status column,
  // which is also what class_management's table used to carry before the two merged.
  it("lists the students with their standing in one table", async () => {
    render(withTestProviders(<ClassProgressView />));

    expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
    expect((await screen.findAllByText("Vắng bài")).length).toBeGreaterThan(0);
    expect(screen.queryByText("Cần chú ý")).not.toBeInTheDocument();
  });
});
