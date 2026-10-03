// Smoke test (PROTOTYPE lane): the read-only page loads the problem and its edit button points at /edit.
import { describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { ProblemInfoView } from "./problem-info-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

describe("ProblemInfoView", () => {
  it("shows the problem and links the edit button to the edit route", async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <ProblemInfoView problemId="121" basePath="/admin/problems" />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "Minimum Window Substring" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "edit" })).toHaveAttribute("href", "/admin/problems/121/edit");
    expect(screen.getByRole("link", { name: "back" })).toHaveAttribute("href", "/admin/problems");
  });
});
