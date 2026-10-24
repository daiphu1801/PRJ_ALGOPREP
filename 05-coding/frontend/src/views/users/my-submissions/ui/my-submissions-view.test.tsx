import { describe, expect, it } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { MySubmissionsView } from "./my-submissions-view";

async function renderView() {
  const result = render(withTestProviders(<MySubmissionsView />));
  await waitFor(() =>
    expect(
      screen.queryByRole("status", { name: "Đang tải..." }),
    ).not.toBeInTheDocument(),
  );
  return result;
}

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
  return screen.getByRole("listbox", { name: label });
}

describe("MySubmissionsView", () => {
  it("renders the submission history table without a stats strip", async () => {
    await renderView();

    expect(screen.getByText("Bài đã nộp")).toBeInTheDocument();
    expect(screen.queryByText("Tổng lượt nộp")).not.toBeInTheDocument();
    expect(screen.getByText("Lịch sử nộp bài")).toBeInTheDocument();
  });

  it("filters the table by verdict", async () => {
    await renderView();

    // The view paints an empty page first and fills it once the query resolves.
    await waitFor(() =>
      expect(screen.getAllByRole("row").length).toBeGreaterThan(2),
    );
    const rowsBefore = screen.getAllByRole("row").length;
    fireEvent.click(
      within(openFilter("Lọc theo kết quả")).getByRole("option", {
        name: "Lỗi biên dịch",
      }),
    );

    await waitFor(() =>
      expect(screen.getAllByRole("row").length).toBeLessThan(rowsBefore),
    );
  });

  it("lets the learner change the rows per page", async () => {
    await renderView();
    const picker = screen.getByRole("button", { name: /^Số dòng mỗi trang/ });
    expect(picker).toHaveTextContent("20");
    fireEvent.click(picker);
    fireEvent.click(screen.getByRole("option", { name: "10" }));
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /^Số dòng mỗi trang/ }),
      ).toHaveTextContent("10"),
    );
  });

  it("offers all six verdict values in the filter (BD Sheet 5 B NO 2)", async () => {
    await renderView();

    const options = within(openFilter("Lọc theo kết quả")).getAllByRole(
      "option",
    );
    // BD lists six values: "Tất cả" plus five verdicts.
    expect(options.map((option) => option.textContent)).toEqual(
      expect.arrayContaining(["Lỗi thực thi"]),
    );
    expect(options).toHaveLength(6);
  });

  it("limits the search box to 100 characters", async () => {
    await renderView();

    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveAttribute(
      "maxlength",
      "100",
    );
  });
});
