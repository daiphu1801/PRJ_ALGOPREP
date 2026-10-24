import { beforeEach, describe, expect, it } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { SavedProblemsView } from "./saved-problems-view";

// The undo offer is a toast action; this stand-in draws just the action buttons of the live toasts.
function ToastActions() {
  return (
    <>
      {useToasts().map((item) =>
        item.action ? (
          <button key={item.id} type="button" onClick={item.action.onClick}>
            {item.action.label}
          </button>
        ) : null,
      )}
    </>
  );
}

beforeEach(() => toast.clear());

// The list arrives through a query: wait for the loading skeleton to go before asserting.
async function renderView() {
  const view = render(
    withTestProviders(
      <>
        <ToastActions />
        <SavedProblemsView />
      </>,
    ),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("status", { name: "Đang tải..." }),
    ).not.toBeInTheDocument(),
  );
  return view;
}

describe("SavedProblemsView", () => {
  it("renders the page title and the bookmarked rows", async () => {
    await renderView();

    expect(screen.getByText("Bài đã lưu")).toBeInTheDocument();
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("removes a row on Bỏ lưu and Hoàn tác restores it with its note (REQ-04, Q2)", async () => {
    await renderView();

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    const [firstUnsaveButton] = screen.getAllByText("Bỏ lưu");
    fireEvent.click(firstUnsaveButton!);

    expect(screen.queryByText("Two Sum")).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("Hoàn tác"));
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(
      screen.getByText("Nhớ dùng HashMap thay vì hai vòng lặp lồng nhau."),
    ).toBeInTheDocument();
  });

  it("keeps the first undo when a second row is unsaved (one toast action per row)", async () => {
    await renderView();

    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    const undoButtons = screen.getAllByText("Hoàn tác");
    expect(undoButtons).toHaveLength(2);
    fireEvent.click(undoButtons[0]!);
    fireEvent.click(screen.getAllByText("Hoàn tác")[0]!);
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("limits the search box to 100 characters", async () => {
    await renderView();

    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveAttribute(
      "maxlength",
      "100",
    );
  });

  it("lets the note of a withdrawn problem be edited (owner decision, BD Sheet 6 C NO 8)", async () => {
    await renderView();

    const withdrawnBadge = screen.getByText("Đã rút xuống");
    const row = withdrawnBadge.closest("tr")!;
    fireEvent.click(row.querySelector("button[title]:not([disabled])")!);
    expect(row.querySelector("textarea")).not.toBeNull();
  });
});
