import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProblemDetailView } from "./problem-detail-view";

let currentProblemId: string | undefined = "1";
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: currentProblemId }),
  useRouter: () => ({ push: vi.fn() }),
}));

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <ProblemDetailView />
    </NextIntlClientProvider>,
  );
}

beforeEach(() => {
  currentProblemId = "1";
  window.localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("ProblemDetailView", () => {
  it("renders the problem statement and the primary actions (Run Code / Submit)", () => {
    renderView();

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText("Run Code")).toBeInTheDocument();
    expect(screen.getByText("Submit")).toBeInTheDocument();
  });

  it("shows a not-found state for an unknown problem id", () => {
    currentProblemId = "does-not-exist";
    renderView();

    expect(screen.getByText("Không tìm thấy bài toán.")).toBeInTheDocument();
  });

  it("renders constraints from the statement Markdown, not from a separate field", () => {
    renderView();

    expect(screen.getByText("Ràng buộc")).toBeInTheDocument();
    expect(
      screen.getByText("Chỉ có đúng một đáp án hợp lệ"),
    ).toBeInTheDocument();
  });

  it("starts the save button from the stored state of the problem", () => {
    renderView();

    expect(screen.getByRole("button", { name: "Bỏ lưu" })).toBeInTheDocument();
  });

  it("shows the effective time and memory limits of the selected language", () => {
    renderView();

    // Java is 2x the base 1000 ms / 256 MB in the mock language config.
    expect(screen.getByText("Time 2s · Memory 512MB")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "C++" }));
    expect(screen.getByText("Time 1s · Memory 256MB")).toBeInTheDocument();
  });

  it("Run Code shows actual output, expected output and metrics per sample testcase", async () => {
    renderView();

    expect(
      screen.getByText(/Còn 9 testcase ẩn, chỉ chạy khi nộp bài/),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Run Code/ }));

    // The sub-screen opens on the first failing sample (case 3) and lists every sample.
    const dialog = within(await screen.findByRole("dialog"));
    expect(await dialog.findByText("Kết quả thực tế")).toBeInTheDocument();
    expect(dialog.getByText("Kết quả mong đợi")).toBeInTheDocument();
    expect(dialog.getByText("[]")).toBeInTheDocument();
    expect(dialog.getByText(/Thời gian \d+ ms · Bộ nhớ \d+ MB/)).toBeInTheDocument();
    expect(dialog.getByText("2/3 testcase mẫu đạt")).toBeInTheDocument();

    fireEvent.click(dialog.getByRole("button", { name: /Case 1/ }));
    expect(dialog.getByText(/Thời gian 3 ms · Bộ nhớ 36 MB/)).toBeInTheDocument();

    fireEvent.click(dialog.getByRole("button", { name: "Đóng" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByText("2/3 testcase mẫu đạt")).toBeInTheDocument();
  });

  it("does not run blank code and leaves the buttons usable", () => {
    renderView();

    fireEvent.change(screen.getByLabelText("Vùng soạn mã"), {
      target: { value: "   " },
    });
    const run = screen.getByRole("button", { name: /Run Code/ });
    fireEvent.click(run);

    expect(run).not.toBeDisabled();
    expect(screen.queryByText("Kết quả thực tế")).not.toBeInTheDocument();
  });

  it("keeps a draft per problem, language and model and says so only when it was stored", () => {
    renderView();

    fireEvent.change(screen.getByLabelText("Vùng soạn mã"), {
      target: { value: "class Solution {}" },
    });
    expect(
      window.localStorage.getItem("algoprep-draft:1:java:function"),
    ).toBe("class Solution {}");
    expect(screen.getByText("Đã lưu trên trình duyệt này")).toBeInTheDocument();
  });

  it("does not claim the draft was saved when the browser blocks storage", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    renderView();

    fireEvent.change(screen.getByLabelText("Vùng soạn mã"), {
      target: { value: "class Solution {}" },
    });
    expect(
      screen.queryByText("Đã lưu trên trình duyệt này"),
    ).not.toBeInTheDocument();
  });
});
