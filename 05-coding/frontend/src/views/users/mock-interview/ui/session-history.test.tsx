import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { SessionHistoryCard, SessionHistoryList } from "./session-history";

describe("session history (mock data)", () => {
  it("lists the finished sessions with date, score and level, each linking to its detail", async () => {
    render(withTestProviders(<SessionHistoryList />));
    expect(await screen.findAllByText(/\/ 10$/)).toHaveLength(8);
    for (const link of screen.getAllByRole("link")) {
      expect(link).toHaveAttribute(
        "href",
        expect.stringMatching(/^\/mock-interview\/history\/sess-/),
      );
    }
  });

  it("the entry card shows five rows and links to the full history screen", async () => {
    render(withTestProviders(<SessionHistoryCard />));
    expect(await screen.findAllByText(/\/ 10$/)).toHaveLength(5);
    expect(screen.getByRole("link", { name: /Xem tất cả/ })).toHaveAttribute(
      "href",
      "/mock-interview/history",
    );
  });
});
