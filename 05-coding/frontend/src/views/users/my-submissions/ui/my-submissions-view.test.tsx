import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { MySubmissionsView } from "./my-submissions-view";

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <MySubmissionsView />
    </NextIntlClientProvider>,
  );
}

describe("MySubmissionsView", () => {
  it("renders the stats strip and the submission history table", () => {
    renderView();

    expect(screen.getByText("Bài đã nộp")).toBeInTheDocument();
    expect(screen.getByText("Tổng lượt nộp")).toBeInTheDocument();
    expect(screen.getByText("Lịch sử nộp bài")).toBeInTheDocument();
  });

  it("filters the table by verdict", () => {
    renderView();

    const rowsBefore = screen.getAllByRole("row").length;
    fireEvent.click(screen.getByRole("button", { name: "Lỗi biên dịch" }));
    const rowsAfter = screen.getAllByRole("row").length;

    expect(rowsAfter).toBeLessThan(rowsBefore);
  });
});
