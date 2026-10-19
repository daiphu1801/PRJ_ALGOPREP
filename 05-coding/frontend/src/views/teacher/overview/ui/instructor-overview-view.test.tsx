// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { InstructorOverviewView } from "./instructor-overview-view";

describe("InstructorOverviewView", () => {
  it("loads the dashboard blocks (classes, assignments)", async () => {
    render(withTestProviders(<InstructorOverviewView />));

    // A class name now appears in several blocks (completion bars, weekly schedule, recent submissions).
    expect(
      (await screen.findAllByText("Lập trình Java K21")).length,
    ).toBeGreaterThan(0);
    expect(screen.getByText("Lớp của tôi")).toBeInTheDocument();
    expect(screen.getByText("Lịch dạy trong tuần")).toBeInTheDocument();
    expect(screen.getByText("Bài nộp gần đây")).toBeInTheDocument();
    expect(screen.getByText("Lượt nộp theo ngày")).toBeInTheDocument();
  });
});
