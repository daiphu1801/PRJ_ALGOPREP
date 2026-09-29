// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassManagementView } from "./class-management-view";

describe("ClassManagementView", () => {
  it("renders the class grid and no student table", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect(await screen.findByText("Lập trình Java K21")).toBeInTheDocument();
    // The student table moved to class_progress on 2026-09-27 — this screen manages classes only.
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
