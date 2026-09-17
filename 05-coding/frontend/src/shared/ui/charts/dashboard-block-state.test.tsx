// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { DashboardBlockState } from "./dashboard-block-state";

// Each of the 9 admin_overview blocks manages its own loading/empty/error state independently
// (02-bd/screens/admin/admin_overview.md section 4) — this pins the 4-way branch that makes that
// true, so a future edit can't quietly collapse it back to a single screen-level state.
describe("DashboardBlockState", () => {
  const baseProps = {
    title: "Test block",
    onRetry: vi.fn(),
    emptyMessage: "empty",
    errorMessage: "error",
    retryLabel: "retry",
  };

  it("shows a skeleton while loading", () => {
    render(
      <DashboardBlockState {...baseProps} isLoading isError={false} isEmpty={false}>
        <p>content</p>
      </DashboardBlockState>,
    );
    expect(screen.queryByText("content")).not.toBeInTheDocument();
  });

  it("shows the error message and a retry action on error", () => {
    render(
      <DashboardBlockState {...baseProps} isLoading={false} isError isEmpty={false}>
        <p>content</p>
      </DashboardBlockState>,
    );
    expect(screen.getByText("error")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "retry" })).toBeInTheDocument();
  });

  it("shows the empty message when there is no error and no data", () => {
    render(
      <DashboardBlockState {...baseProps} isLoading={false} isError={false} isEmpty>
        <p>content</p>
      </DashboardBlockState>,
    );
    expect(screen.getByText("empty")).toBeInTheDocument();
  });

  it("renders children on success", () => {
    render(
      <DashboardBlockState {...baseProps} isLoading={false} isError={false} isEmpty={false}>
        <p>content</p>
      </DashboardBlockState>,
    );
    expect(screen.getByText("content")).toBeInTheDocument();
  });
});
