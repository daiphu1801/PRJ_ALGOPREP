// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { InstructorGradingView } from "./instructor-grading-view";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

describe("InstructorGradingView", () => {
  it("loads the pending manual-grading queue by default", async () => {
    render(withTestProviders(<InstructorGradingView />));

    expect(await screen.findByText("Trần Thị Bích")).toBeInTheDocument();
    expect((await screen.findAllByText("Chấm bài")).length).toBeGreaterThan(0);
  });
});
