import { describe, expect, it } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { MockInterviewHistoryView } from "./history-view";
import { MockInterviewSessionDetailView } from "./session-detail-view";

// Mock mode: 12 canned sessions of every source, level and score band, abandoned ones without a score.

async function renderList() {
  render(withTestProviders(<MockInterviewHistoryView />));
  await screen.findByText(/Hiển thị 1 đến 10 trong 12 phiên/);
}

// Filters are a button that opens a listbox; open it first, then pick from the options.
function pick(label: string, option: string | RegExp) {
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
  fireEvent.click(
    within(screen.getByRole("listbox", { name: label })).getByRole("option", {
      name: option,
    }),
  );
}

const dataRows = () => screen.getAllByRole("row").slice(1);

describe("MockInterviewHistoryView", () => {
  it("shows the first page of the grid with a detail link on every row", async () => {
    await renderList();
    expect(dataRows()).toHaveLength(10);
    expect(
      screen.getAllByRole("link", { name: "Xem chi tiết" })[0],
    ).toHaveAttribute(
      "href",
      expect.stringMatching(/^\/mock-interview\/history\/hist-/),
    );
  });

  it("filters by status: only abandoned sessions, which have no score", async () => {
    await renderList();
    pick("Trạng thái", "Bỏ dở");
    await screen.findByText(/Hiển thị 1 đến 2 trong 2 phiên/);
    expect(dataRows()).toHaveLength(2);
    for (const row of dataRows()) {
      expect(within(row).getByText("Bỏ dở")).toBeInTheDocument();
      expect(within(row).queryByText(/\/ 10/)).toBeNull();
    }
  });

  it("filters by score band and by source", async () => {
    await renderList();
    pick("Mức điểm", /^Yếu/);
    await waitFor(() => expect(dataRows().length).toBeLessThan(10));
    // An empty list would pass the loops below without checking anything.
    expect(dataRows().length).toBeGreaterThan(0);
    for (const row of dataRows()) {
      expect(within(row).getByText("Yếu")).toBeInTheDocument();
    }
    pick("Nguồn", "Kho câu hỏi");
    await waitFor(() => {
      expect(dataRows().length).toBeGreaterThan(0);
      for (const row of dataRows()) {
        expect(within(row).getByText("Kho câu hỏi")).toBeInTheDocument();
      }
    });
  });

  it("searches by question text after the pause, and the empty result says so", async () => {
    await renderList();
    fireEvent.change(screen.getByLabelText("Tìm phiên phỏng vấn"), {
      target: { value: "ngân hàng số 4" },
    });
    expect(
      await screen.findByText(/Hiển thị 1 đến 1 trong 1 phiên/),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Tìm phiên phỏng vấn"), {
      target: { value: "không có gì khớp" },
    });
    expect(
      await screen.findByText(/Chưa có phiên nào khớp/),
    ).toBeInTheDocument();
  });

  it("sorts by score when the header is pressed and pages forward", async () => {
    await renderList();
    fireEvent.click(screen.getByRole("button", { name: /^Điểm/ }));
    await waitFor(() =>
      expect(
        screen.getByRole("columnheader", { name: /Điểm/ }),
      ).toHaveAttribute("aria-sort", "ascending"),
    );
    fireEvent.click(screen.getByRole("button", { name: "Sau" }));
    await screen.findByText(/Hiển thị 11 đến 12 trong 12 phiên/);
    expect(dataRows()).toHaveLength(2);
  });
});

describe("MockInterviewSessionDetailView", () => {
  it("shows the scorecard on top and the conversation below for a completed session", async () => {
    render(
      withTestProviders(<MockInterviewSessionDetailView sessionId="hist-1" />),
    );
    expect(await screen.findByText("Cuộc hội thoại")).toBeInTheDocument();
    expect(screen.getByText("Nhận xét tổng quan")).toBeInTheDocument();
    expect(screen.getByText(/Mình dùng bảng băm/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Luyện lại/ })).toBeNull();
  });

  it("shows only the conversation for an abandoned session", async () => {
    render(
      withTestProviders(<MockInterviewSessionDetailView sessionId="hist-6" />),
    );
    expect(
      await screen.findByText(/bỏ dở nên không có điểm/),
    ).toBeInTheDocument();
    expect(screen.queryByText("Nhận xét tổng quan")).toBeNull();
    expect(screen.getByText(/Mình dùng bảng băm/)).toBeInTheDocument();
  });
});
