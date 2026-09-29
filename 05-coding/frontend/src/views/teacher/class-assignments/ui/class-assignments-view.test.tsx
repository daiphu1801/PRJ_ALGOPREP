// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassAssignmentsView } from "./class-assignments-view";

describe("ClassAssignmentsView", () => {
  it("loads assigned problems and renders the table", async () => {
    render(withTestProviders(<ClassAssignmentsView />));

    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect((await screen.findAllByText("Bài tập của tôi")).length).toBeGreaterThan(0);
  });
});
