import { describe, expect, it } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
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

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
  return screen.getByRole("listbox", { name: label });
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
    fireEvent.click(
      within(openFilter("Lọc theo kết quả")).getByRole("option", {
        name: "Lỗi biên dịch",
      }),
    );
    const rowsAfter = screen.getAllByRole("row").length;

    expect(rowsAfter).toBeLessThan(rowsBefore);
  });

  it("lets the learner change the rows per page", () => {
    renderView();
    const picker = screen.getByRole("button", { name: /^Số dòng mỗi trang/ });
    expect(picker).toHaveTextContent("20");
    fireEvent.click(picker);
    fireEvent.click(screen.getByRole("option", { name: "10" }));
    expect(
      screen.getByRole("button", { name: /^Số dòng mỗi trang/ }),
    ).toHaveTextContent("10");
  });
});
