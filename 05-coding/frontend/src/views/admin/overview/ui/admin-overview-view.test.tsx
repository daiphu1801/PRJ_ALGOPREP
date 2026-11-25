// Mock-mode render of the whole admin overview grid: every block loads its own data, and each block
// owns its error state with a retry. The query hooks are wrapped so one test can force failures.
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

const force = vi.hoisted(() => ({
  mode: "none" as "none" | "error" | "empty",
  refetch: vi.fn(),
}));

vi.mock("@/shared/i18n/server", () => ({
  getT: async () => (key: string) => key,
}));

vi.mock("../api", async (importOriginal) => {
  const actual = await importOriginal<Record<string, unknown>>();
  const wrapped: Record<string, unknown> = { ...actual };
  for (const name of Object.keys(actual)) {
    if (!name.startsWith("use")) continue;
    wrapped[name] = (...args: unknown[]) => {
      const real = (actual[name] as (...a: unknown[]) => unknown)(...args);
      if (force.mode === "error")
        return {
          isLoading: false,
          isError: true,
          data: undefined,
          refetch: force.refetch,
        };
      if (force.mode === "empty" && name === "useTopProblems")
        return {
          isLoading: false,
          isError: false,
          refetch: force.refetch,
          data: {
            items: [],
            monthTotal: 0,
            points: [],
            groups: [{ label: "x", bars: [{ value: 0, label: "a" }] }],
            legend: [],
          },
        };
      return real;
    };
  }
  return wrapped;
});

import { AdminOverviewView } from "./admin-overview-view";

const t = viMessages.adminOverview;

async function renderOverview() {
  return render(withTestProviders(await AdminOverviewView()));
}

beforeEach(() => {
  force.mode = "none";
  force.refetch.mockReset();
});

describe("AdminOverviewView", () => {
  it("renders the screen-reader heading and the title of every data block", async () => {
    await renderOverview();

    expect(
      screen.getByRole("heading", { level: 1, name: "title" }),
    ).toBeInTheDocument();
    for (const key of [
      "verdictDistribution",
      "submissionsByDay",
      "topProblems",
      "submissionsByMonth",
      "submissionsByLanguage",
    ] as const) {
      expect(await screen.findByText(t.blocks[key])).toBeInTheDocument();
    }
  });

  it("fills the three stat cards, the day grid totals, the ranked list and its link", async () => {
    await renderOverview();

    // Stat cards carry their labels once the mock data has loaded.
    expect(
      await screen.findByText(t.blocks.submissionsSummary),
    ).toBeInTheDocument();
    expect(screen.getByText(t.blocks.activeUsersSummary)).toBeInTheDocument();
    expect(
      screen.getByText(t.blocks.passwordResetsSummary),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(
        new RegExp(`${t.blocks.weekTotal} · ${t.blocks.dailyAverage}: \\d+`),
      ),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("link", { name: t.blocks.viewAll }),
    ).toHaveAttribute("href", "/admin/problems");
  });

  it("shows an error with a retry in every block that failed, and retry refetches", async () => {
    force.mode = "error";
    await renderOverview();

    const messages = await screen.findAllByText(t.errorGeneric);
    // 3 stat cards + 3 other blocks.
    expect(messages.length).toBeGreaterThanOrEqual(6);
    fireEvent.click(screen.getAllByRole("button", { name: t.retry })[0]!);
    expect(force.refetch).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getAllByRole("button", { name: t.retry })[3]!);
    await waitFor(() => expect(force.refetch).toHaveBeenCalledTimes(2));
  });

  it("shows the empty message for blocks whose data is all zero or has no rows", async () => {
    force.mode = "empty";
    await renderOverview();

    const empties = await screen.findAllByText(t.emptyGeneric);
    expect(empties).toHaveLength(1);
  });
});
