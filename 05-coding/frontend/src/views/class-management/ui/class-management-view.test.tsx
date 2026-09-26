// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassManagementView } from "./class-management-view";

describe("ClassManagementView", () => {
  it("loads the instructor's classes and renders the class grid", async () => {
    render(withTestProviders(<ClassManagementView />));

    expect((await screen.findAllByText("Lập trình Java K21")).length).toBeGreaterThan(0);
    expect(screen.getByText("Lớp của tôi")).toBeInTheDocument();
  });
});
