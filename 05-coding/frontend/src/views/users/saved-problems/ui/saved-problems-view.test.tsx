import { beforeEach, describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import { toast, useToasts } from "@/shared/lib/toast-store";
import messages from "../../../../../messages/vi.json";
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

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <ToastActions />
      <SavedProblemsView />
    </NextIntlClientProvider>,
  );
}

describe("SavedProblemsView", () => {
  it("renders the page title and the bookmarked rows", () => {
    renderView();

    expect(screen.getByText("Bài đã lưu")).toBeInTheDocument();
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("removes a row on Bỏ lưu and Hoàn tác restores it with its note (REQ-04, Q2)", () => {
    renderView();

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

  it("keeps the first undo when a second row is unsaved (one toast action per row)", () => {
    renderView();

    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    const undoButtons = screen.getAllByText("Hoàn tác");
    expect(undoButtons).toHaveLength(2);
    fireEvent.click(undoButtons[0]!);
    fireEvent.click(screen.getAllByText("Hoàn tác")[0]!);
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("limits the search box to 100 characters", () => {
    renderView();

    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveAttribute(
      "maxlength",
      "100",
    );
  });

  it("lets the note of a withdrawn problem be edited (owner decision, BD Sheet 6 C NO 8)", () => {
    renderView();

    const withdrawnBadge = screen.getByText("Đã rút xuống");
    const row = withdrawnBadge.closest("tr")!;
    fireEvent.click(row.querySelector("button[title]:not([disabled])")!);
    expect(row.querySelector("textarea")).not.toBeNull();
  });
});
