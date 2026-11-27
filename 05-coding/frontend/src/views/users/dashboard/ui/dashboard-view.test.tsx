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
    // my_progress half, merged in by DEC-2026-0927-student-area-merge-and-shared-shell.
    expect(
      await screen.findByRole("table", { name: "Theo chủ đề" }),
    ).toBeInTheDocument();
  });

  // DEC-2026-1123-student-dashboard-trim: each fact is shown once. The radar and "Nên ưu tiên" repeated the topic
  // table and the greeting, the 12-month heatmap repeated the daily chart.
  it("no longer shows the radar, Nên ưu tiên or the 12-month heatmap", async () => {
    renderView();
    await screen.findByRole("table", { name: "Theo chủ đề" });

    expect(
      screen.queryByRole("region", { name: "Năng lực theo chủ đề" }),
    ).toBeNull();
    expect(screen.queryByRole("heading", { name: "Nên ưu tiên" })).toBeNull();
    expect(screen.queryByText(/Hoạt động 12 tháng/)).toBeNull();
  });

  it("drives every range-aware block from the one screen-level control", async () => {
    renderView();

    // Scoped to the chart's own region, and counted on the line's points rather than on dots: a dense
    // series draws no dots (they would stretch into ellipses).
    const plottedPoints = () =>
      screen
        .getByRole("region", { name: "Bài nộp theo ngày" })
        .querySelector("polyline")
        ?.getAttribute("points")
        ?.split(" ").length ?? 0;

    const table = await screen.findByRole("table", { name: "Theo chủ đề" });
    const topicRowBefore = within(table).getAllByRole("row")[1]?.textContent;
    // 14 plotted points on "30d", 7 on "7d" — a deterministic witness that the chart replotted.
    await waitFor(() => expect(plottedPoints()).toBe(14));

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

  // Owner decision 2026-10-18: the range also filters the interview block; DEC-2026-1123-student-dashboard-trim
  // turned each session into one line (score and weakest criterion) instead of four bars.
  it("filters the interview block by range and shows one line per session", async () => {
    renderView();

    // 30d: 6 sessions in range, the card lists the last 3 (owner D3).
    expect(await screen.findAllByText(/^Phiên ngày /)).toHaveLength(3);
    expect(screen.getAllByText(/^Cần cải thiện: /)).toHaveLength(3);
    expect(
      screen.queryAllByRole("progressbar", { name: "Độ rõ ràng" }),
    ).toHaveLength(0);

    fireEvent.click(screen.getByRole("button", { name: "7 ngày" }));

    await waitFor(() =>
      expect(screen.getAllByText(/^Phiên ngày /)).toHaveLength(2),
    );
  });
});
