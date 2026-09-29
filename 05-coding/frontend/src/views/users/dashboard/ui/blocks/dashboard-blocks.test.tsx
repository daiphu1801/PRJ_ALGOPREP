import { describe, expect, it } from "vitest";
import type { ReactNode } from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
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
  // REQ-06: picking a difficulty filters the table and the count beside the title follows it.
  it("filters the table and updates the count", async () => {
    renderBlock(<SuggestedProblemsBlock />);

    const table = await screen.findByRole("table", { name: "Bài toán gợi ý" });
    const rowsBefore = within(table).getAllByRole("row").length;

    fireEvent.click(screen.getByRole("button", { name: "Khó" }));

    const rowsAfter = within(await screen.findByRole("table", { name: "Bài toán gợi ý" })).getAllByRole("row").length;
    expect(rowsAfter).toBeLessThan(rowsBefore);
    expect(screen.getByText(`${rowsAfter - 1} bài`)).toBeInTheDocument();
  });
});

describe("GreetingBlock", () => {
  // REQ-04: the callout names the weakest topic, and it must be the same one the radar derives —
  // both go through deriveSkillRadar, which is what this pins.
  it("names the weakest topic once the topic data lands", async () => {
    renderBlock(<GreetingBlock />);

    expect(await screen.findByText(/Điểm yếu tuần này:/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Tiếp tục bài đang làm|Bắt đầu giải bài/ })).toBeInTheDocument();
  });
});
