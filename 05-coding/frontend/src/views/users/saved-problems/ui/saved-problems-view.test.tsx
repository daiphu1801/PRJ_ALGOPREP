// USR0103 in mock mode: the mock answers the same contract as the real endpoints, so these tests prove
// the screen's own behaviour (what it sends, how it undoes, what it says when something fails).
import { beforeEach, describe, expect, it } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { resetSavedMock } from "@/entities/problem";
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

beforeEach(() => {
  resetSavedMock();
  act(() => toast.clear());
});

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
  it("renders the page title, the bookmarked rows and the totals from the server's meta", async () => {
    await renderView();

    expect(screen.getByText("Bài đã lưu")).toBeInTheDocument();
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText("3 bài đã lưu, 1 bài đã giải")).toBeInTheDocument();
  });

  it("removes a row on Bỏ lưu and Hoàn tác restores it with its note (REQ-04, Q2)", async () => {
    await renderView();

    const [firstUnsaveButton] = screen.getAllByText("Bỏ lưu");
    fireEvent.click(firstUnsaveButton!);

    await waitFor(() =>
      expect(screen.queryByText("Two Sum")).not.toBeInTheDocument(),
    );
    expect(screen.getByText("2 bài đã lưu, 0 bài đã giải")).toBeInTheDocument();

    fireEvent.click(await screen.findByText("Hoàn tác"));
    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(
      screen.getByText("Nhớ dùng HashMap thay vì hai vòng lặp lồng nhau."),
    ).toBeInTheDocument();
  });

  it("keeps the first undo when a second row is unsaved (one toast action per row)", async () => {
    await renderView();

    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    await waitFor(() => expect(screen.getAllByText("Bỏ lưu")).toHaveLength(2));
    fireEvent.click(screen.getAllByText("Bỏ lưu")[0]!);
    await waitFor(() =>
      expect(screen.getAllByText("Hoàn tác")).toHaveLength(2),
    );
    // A toast action stays after it is used, so take both buttons now and press each once.
    const undo = screen.getAllByText("Hoàn tác");
    fireEvent.click(undo[0]!);
    await waitFor(() => expect(screen.getAllByText("Bỏ lưu")).toHaveLength(2));
    fireEvent.click(undo[1]!);
    await waitFor(() => expect(screen.getAllByText("Bỏ lưu")).toHaveLength(3));
  });

  it("filters on the server: a word of the title leaves one row", async () => {
    await renderView();

    fireEvent.change(screen.getByRole("textbox", { name: /Tìm/ }), {
      target: { value: "word break" },
    });
    await waitFor(() =>
      expect(screen.queryByText("Two Sum")).not.toBeInTheDocument(),
    );
    expect(screen.getByText("Word Break")).toBeInTheDocument();
  });

  it("limits the search box to 100 characters", async () => {
    await renderView();

    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveAttribute(
      "maxlength",
      "100",
    );
  });

  it("lets the note of a withdrawn problem be edited and saves it (owner decision, BD Sheet 6 C NO 8)", async () => {
    await renderView();

    const withdrawnBadge = screen.getByText("Đã rút xuống");
    const row = withdrawnBadge.closest("tr")!;
    fireEvent.click(row.querySelector("button[title]:not([disabled])")!);
    const box = row.querySelector("textarea")!;
    fireEvent.change(box, { target: { value: "  ghi chú mới  " } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    expect(await screen.findByText("ghi chú mới")).toBeInTheDocument();
  });

  it("a withdrawn problem cannot be restored after an unsave, and the screen says why", async () => {
    const toasts = renderHook(() => useToasts());
    await renderView();
    const row = screen.getByText("Đã rút xuống").closest("tr")!;
    fireEvent.click(within(row).getByRole("button", { name: "Bỏ lưu" }));
    fireEvent.click(await screen.findByText("Hoàn tác"));
    // PRB-010 from the server: the vi text, not the raw code.
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({
        tone: "error",
        message: "Không tìm thấy bài toán hoặc bài không còn được mở.",
      }),
    );
  });
});
