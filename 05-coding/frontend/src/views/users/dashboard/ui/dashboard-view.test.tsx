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

  // Owner decision 2026-10-13: the greeting and "Nên ưu tiên" never name different weakest topics.
  it("pins the greeting's weakest topic at the top of Nên ưu tiên", async () => {
    renderView();

    const greeting = await screen.findByText(/Điểm yếu tuần này:/);
    const topic = /Điểm yếu tuần này: (.+) — năng lực/.exec(
      greeting.textContent ?? "",
    )![1]!;

    await screen.findByRole("table", { name: "Theo chủ đề" });
    const firstFocus = await waitFor(() => {
      const links = screen
        .getAllByRole("link")
        .filter((a) =>
          a.getAttribute("href")?.startsWith("/problems?topicId="),
        );
      expect(links.length).toBeGreaterThan(0);
      return links[0]!;
    });
    expect(firstFocus.textContent).toContain(topic);
    expect(firstFocus.textContent).toContain("thấp nhất");
  });

  it("drives every range-aware block from the one screen-level control", async () => {
    renderView();

    // Scoped to the chart's own region, and counted on the line's points rather than on dots: a dense
    // series draws no dots (they would stretch into ellipses), and the radar beside it draws circles.
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

  // Owner decision 2026-10-18: the range also filters the interview block, scores are 0 to 10, and
  // each session card draws one bar per rubric criterion.
  it("filters the interview block by range and draws 4 rubric bars per session", async () => {
    renderView();

    // 30d: 6 sessions in range, the card lists the last 3 (owner D3), each with its own 4 bars.
    expect(await screen.findAllByText(/^Phiên ngày /)).toHaveLength(3);
    expect(screen.getAllByText(/ \/ 10$/).length).toBeGreaterThanOrEqual(3);
    expect(screen.getAllByRole("progressbar", { name: "Độ rõ ràng" })).toHaveLength(3);

    fireEvent.click(screen.getByRole("button", { name: "7 ngày" }));

    await waitFor(() =>
      expect(screen.getAllByText(/^Phiên ngày /)).toHaveLength(2),
    );
    expect(screen.getAllByRole("progressbar", { name: "Độ rõ ràng" })).toHaveLength(2);
  });
});
