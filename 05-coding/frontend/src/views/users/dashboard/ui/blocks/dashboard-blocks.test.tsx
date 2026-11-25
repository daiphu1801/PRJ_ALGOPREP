import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import {
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../../messages/vi.json";
import { GreetingBlock } from "./greeting-block";
import { SuggestedProblemsBlock } from "./suggested-problems-block";

/**
 * Block-level interaction that belongs to one block only. The screen-wide range control moved to
 * the view when `my_progress` merged in, so its test lives in `../dashboard-view.test.tsx`.
 */
function renderBlock(block: ReactNode) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <NextIntlClientProvider locale="vi" messages={messages}>
        {block}
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("SuggestedProblemsBlock", () => {
  // DEC-2026-1123-student-dashboard-trim: no filters on the dashboard, at most 5 rows, never a solved problem,
  // and a way to the full problem list.
  it("lists at most five unsolved problems and links to the problem list", async () => {
    renderBlock(<SuggestedProblemsBlock />);

    const table = await screen.findByRole("table", { name: "Bài toán gợi ý" });
    await waitFor(() =>
      expect(within(table).getAllByRole("row").length).toBeGreaterThan(1),
    );
    const rows = within(table).getAllByRole("row").slice(1);
    expect(rows.length).toBeLessThanOrEqual(5);
    expect(within(table).queryByRole("img", { name: /Đã giải/ })).toBeNull();
    expect(screen.queryByRole("button", { name: "Khó" })).toBeNull();
    expect(screen.getByRole("link", { name: /Tất cả/ })).toHaveAttribute(
      "href",
      "/problems",
    );
  });
});

describe("GreetingBlock", () => {
  // REQ-04: the callout names the weakest topic over every topic (scoreTopics, then pickWeakestTopic).
  it("names the weakest topic once the topic data lands", async () => {
    renderBlock(<GreetingBlock />);

    expect(await screen.findByText(/Điểm yếu tuần này:/)).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: /Tiếp tục bài đang làm|Bắt đầu giải bài/,
      }),
    ).toBeInTheDocument();
  });
});
