// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassProgressView } from "./class-progress-view";

describe("ClassProgressView", () => {
  it("loads the attention list and the student table", async () => {
    render(withTestProviders(<ClassProgressView />));

    expect(await screen.findByText("Cần chú ý")).toBeInTheDocument();
    expect((await screen.findAllByText("Không nộp bài 9 ngày")).length).toBeGreaterThan(0);
  });
});
