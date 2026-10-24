// The usage screen reads the same budget store the limits screen writes (BD ADM0302 Sheet 5 Khu vực D),
// so a new cap or a lock shows here without a reload. `useT` returns the key.
import { act, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { aiTokenBudget } from "@/entities/ai-budget";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
}));

import { AdminAiUsageView } from "./ai-usage";

const withQuery = (ui: ReactElement) => (
  <QueryClientProvider client={new QueryClient()}>{ui}</QueryClientProvider>
);

afterEach(() => {
  act(() =>
    aiTokenBudget.set({
      limitTokens: 52_000_000,
      usedTokens: 38_400_000,
      locked: false,
    }),
  );
});

describe("AI usage budget panel", () => {
  it("shows the projected run-out date while open and the lock once ADM0303 locks the budget", async () => {
    render(withQuery(<AdminAiUsageView />));
    expect(await screen.findByText("budgetRunOut")).toBeInTheDocument();
    expect(screen.queryByText("budgetLocked")).toBeNull();

    act(() => aiTokenBudget.set({ limitTokens: 30_000_000, locked: true }));
    expect(screen.getByText("budgetLocked")).toBeInTheDocument();
    expect(screen.getByText("alert.budgetLocked.title")).toBeInTheDocument();
    expect(screen.queryByText("alert.budgetRunningOut.title")).toBeNull();
    expect(screen.queryByText("budgetRunOut")).toBeNull();
    // Over the cap, the bar is clamped at 100 and nothing is left.
    expect(
      screen.getByRole("progressbar", { name: "budgetTitle" }),
    ).toHaveAttribute("aria-valuenow", "100");
  });

  it("has no export button and no footnote pointing at the old screen name", async () => {
    render(withQuery(<AdminAiUsageView />));
    await screen.findByText("budgetRunOut");
    expect(screen.queryByRole("button", { name: "export" })).toBeNull();
    expect(screen.queryByText("alertsFootnote")).toBeNull();
    expect(screen.queryByText("alert.testcasePromptCost.title")).toBeNull();
  });
});
