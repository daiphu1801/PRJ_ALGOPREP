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

describe("ProblemInfoView spec block (read-only, SHR0202 Q20)", () => {
  it("shows the starter line of each language, the I/O formats and the matching strategy", async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <ProblemInfoView problemId="121" basePath="/admin/problems" />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("specTitle")).toBeInTheDocument();
    expect(screen.getByText("def min_window(s: str, t: str) -> str:")).toBeInTheDocument();
    expect(screen.getByText("public String minWindow(String s, String t)")).toBeInTheDocument();
    expect(screen.getByText("string minWindow(string s, string t)")).toBeInTheDocument();
    expect(screen.getByText("spec.stdinFormat")).toBeInTheDocument();
    expect(screen.getByText("EXACT")).toBeInTheDocument();
  });
});
