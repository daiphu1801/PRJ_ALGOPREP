// USR0601 blocks against the REAL api path for #47 and #55: the figures the server gives are shown,
// the ones owned by modules without an endpoint yet read "-", and a failed call shows the retry state.
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

// Import after the mocks.
import { ApiError } from "@/shared/api";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ActivityBlock } from "./activity-block";
import { StatCardsRow } from "./stat-cards-row";

const m = viMessages.dashboard;

function summary(over: Record<string, unknown> = {}) {
  return {
    solvedProblemCount: 3,
    totalSubmissions: 10,
    acceptedCount: 5,
    acceptanceRatePercent: 63,
    firstTryAcceptedCount: 2,
    topLanguage: "PYTHON",
    ...over,
  };
}

function routes(handlers: Record<string, () => unknown>) {
  apiMock.get.mockImplementation(async (path: string) => {
    const handler = handlers[path];
    if (!handler) throw new Error("not implemented");
    return handler();
  });
}

describe("StatCardsRow with the real API", () => {
  beforeEach(() => {
    resetApiMock();
  });

  it("shows the solved count and the server's acceptance rate, not accepted / total", async () => {
    routes({ "/identity/me/progress/summary": () => summary() });

    render(withTestProviders(<StatCardsRow />));

    // 5 / 10 would read 50%; the server leaves SYSTEM_ERROR out of the denominator and says 63.
    expect(await screen.findByText("63%")).toBeInTheDocument();
    expect(screen.queryByText("50%")).not.toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("draws a dash for the streak while that figure has no endpoint", async () => {
    routes({ "/identity/me/progress/summary": () => summary() });

    render(withTestProviders(<StatCardsRow />));

    const streak = await screen.findByRole("region", {
      name: m.stats.regionLabel.replace("{metric}", m.stats.streak),
    });
    await waitFor(() => expect(streak).toHaveTextContent(m.stats.streak));
    // Label, then the dash straight away: no "ngày" unit follows an unknown figure.
    expect(streak.textContent).toMatch(new RegExp(`^${m.stats.streak}-`));
  });

  it("shows the empty rate message when no submission is eligible", async () => {
    routes({
      "/identity/me/progress/summary": () =>
        summary({ acceptanceRatePercent: null, acceptedCount: 0 }),
    });

    render(withTestProviders(<StatCardsRow />));

    expect(await screen.findByText(m.stats.acRateEmpty)).toBeInTheDocument();
  });

  it("shows the generic error with a retry button when #47 fails", async () => {
    routes({
      "/identity/me/progress/summary": () => {
        throw new ApiError("IDT-503", 503, "down");
      },
    });

    render(withTestProviders(<StatCardsRow />));

    expect((await screen.findAllByText(m.errorGeneric)).length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByRole("button", { name: m.retry }).length,
    ).toBeGreaterThan(0);
  });
});

describe("ActivityBlock with the real API", () => {
  beforeEach(() => {
    resetApiMock();
  });

  function days(active: Record<number, number>) {
    const end = Date.UTC(2026, 10, 5);
    return Array.from({ length: 364 }, (_, i) => ({
      day: new Date(end - (363 - i) * 86_400_000).toISOString().slice(0, 10),
      submissionCount: active[363 - i] ?? 0,
    }));
  }

  it("counts the days that have submissions", async () => {
    const list = days({ 0: 3, 5: 1 });
    routes({
      "/identity/me/activity-calendar": () => ({
        from: list[0]?.day,
        to: list[363]?.day,
        days: list,
      }),
    });

    render(withTestProviders(<ActivityBlock />));

    expect(
      await screen.findByText(m.activity.summary.replace("{days}", "2")),
    ).toBeInTheDocument();
  });

  it("shows the empty message for a year with no submission", async () => {
    const list = days({});
    routes({
      "/identity/me/activity-calendar": () => ({
        from: list[0]?.day,
        to: list[363]?.day,
        days: list,
      }),
    });

    render(withTestProviders(<ActivityBlock />));

    expect(await screen.findByText(m.activity.empty)).toBeInTheDocument();
  });

  it("shows the error state when #55 fails", async () => {
    routes({
      "/identity/me/activity-calendar": () => {
        throw new ApiError("IDT-503", 503, "down");
      },
    });

    render(withTestProviders(<ActivityBlock />));

    expect(await screen.findByText(m.errorGeneric)).toBeInTheDocument();
  });
});
