// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { InstructorOverviewView } from "./instructor-overview-view";

describe("InstructorOverviewView", () => {
  it("loads the dashboard blocks (classes, pending grading, assignments)", async () => {
    render(withTestProviders(<InstructorOverviewView />));

    expect(await screen.findByText("Lập trình Java K21")).toBeInTheDocument();
    expect(screen.getByText("Lớp của tôi")).toBeInTheDocument();
  });
});
