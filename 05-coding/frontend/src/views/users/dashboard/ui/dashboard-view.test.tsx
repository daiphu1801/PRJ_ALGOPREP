import { describe, expect, it } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { DashboardView } from "./dashboard-view";

function renderView() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        <DashboardView />
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("DashboardView", () => {
  it("shows both halves of the merged screen", async () => {
    renderView();

    // Dashboard half.
    expect(await screen.findByText(/Điểm yếu tuần này:/)).toBeInTheDocument();
    expect(
      screen.getByRole("region", { name: "Năng lực theo chủ đề" }),
    ).toBeInTheDocument();
    // my_progress half, merged in by DEC-2026-0927-student-area-merge-and-shared-shell.
    expect(
      await screen.findByRole("table", { name: "Theo chủ đề" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Nên ưu tiên" }),
    ).toBeInTheDocument();
  });

  it("drives every range-aware block from the one screen-level control", async () => {
    renderView();

    // Scoped to the chart's own region: the radar in the next column also draws <circle>, so a
    // container-wide query would count both and pass for the wrong reason.
    const plottedPoints = () =>
      screen
        .getByRole("region", { name: "Bài nộp theo ngày" })
        .querySelectorAll("svg circle").length;

    const table = await screen.findByRole("table", { name: "Theo chủ đề" });
    const topicRowBefore = within(table).getAllByRole("row")[1]?.textContent;
    // 15 plotted points on "30d", 7 on "7d" — a deterministic witness that the chart replotted.
    await waitFor(() => expect(plottedPoints()).toBe(15));

    fireEvent.click(screen.getByRole("button", { name: "7 ngày" }));

    // Both the merged topic table and the dashboard chart follow the same control — that is the
    // whole point of lifting it out of the chart block during the merge.
    await waitFor(() => expect(plottedPoints()).toBe(7));
    await waitFor(() =>
      expect(
        within(screen.getByRole("table", { name: "Theo chủ đề" })).getAllByRole(
          "row",
        )[1]?.textContent,
      ).not.toBe(topicRowBefore),
    );
  });
});
