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
