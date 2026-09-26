// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { SolutionReviewView } from "./solution-review-view";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

function renderView() {
  return render(withTestProviders(<SolutionReviewView />));
}

describe("SolutionReviewView", () => {
  it("shows the report once the simulated load resolves (BD Sheet 5 Khu vực I, ready state)", async () => {
    renderView();

    expect(await screen.findByText("1. Two Sum")).toBeInTheDocument();
    expect(screen.getByText("Phân tích bài giải")).toBeInTheDocument();
    // Educational disclaimer (F5-18) must be shown alongside the report.
    expect(screen.getByText(/Phản hồi để học/)).toBeInTheDocument();
  });
});
