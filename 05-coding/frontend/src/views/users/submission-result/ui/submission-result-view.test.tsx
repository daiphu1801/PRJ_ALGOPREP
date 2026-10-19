import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { SubmissionResultView } from "./submission-result-view";

let currentSubmissionId = "SB-90412";
vi.mock("next/navigation", () => ({
  useParams: () => ({ submissionId: currentSubmissionId }),
}));

function renderView(area?: "instructor") {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <SubmissionResultView area={area} />
    </NextIntlClientProvider>,
  );
}

describe("SubmissionResultView", () => {
  it("renders the verdict and testcase table for an ACCEPTED submission", () => {
    currentSubmissionId = "SB-90412";
    renderView();

    expect(screen.getByText("Đã chấp nhận")).toBeInTheDocument();
    expect(screen.getByText("Kết quả từng testcase")).toBeInTheDocument();
  });

  it("hides the testcase table entirely on COMPILE_ERROR and shows the compile log instead", () => {
    currentSubmissionId = "SB-90403";
    renderView();

    expect(screen.getByText("Lỗi biên dịch")).toBeInTheDocument();
    // The testcase table (identified by its accessible caption) must not be in the document at all —
    // not just empty — per BD Sheet 6 Khu vực D NO 1.
    expect(screen.queryByText("Kết quả từng testcase")).not.toBeInTheDocument();
    expect(
      screen.getByText(/expected ';' after expression/),
    ).toBeInTheDocument();
  });

  it("shows a not-found state for an unknown submission id", () => {
    currentSubmissionId = "does-not-exist";
    renderView();

    expect(screen.getByText("Không tìm thấy bài nộp.")).toBeInTheDocument();
  });

  it("reads read-only for an instructor: source visible, no learner actions (DEC-2026-1013)", () => {
    currentSubmissionId = "SB-90412";
    renderView("instructor");

    expect(
      screen.getByText(/Chỉ đọc: bạn đang xem bài nộp/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Bài nộp" })).toHaveAttribute(
      "href",
      "/instructor/submissions",
    );
    expect(
      screen.queryByText("Phân tích bài giải (AI)"),
    ).not.toBeInTheDocument();
  });

  it("returns not-found to an instructor for a submission outside their classes", () => {
    currentSubmissionId = "SB-99999";
    renderView("instructor");

    expect(screen.getByText("Không tìm thấy bài nộp.")).toBeInTheDocument();
  });

  it("prints run stats as one text line in the banner, no stat-card strip (owner 2026-10-13)", () => {
    currentSubmissionId = "SB-90412";
    renderView();

    expect(screen.getByText(/Runtime 42 ms · Memory 15\.0 MB · Beats/)).toBeInTheDocument();
  });

  it("marks the expand toggle with aria-expanded for a sample testcase", () => {
    currentSubmissionId = "SB-90412";
    renderView();

    const [first] = screen.getAllByRole("button", { name: "Chi tiết" });
    expect(first).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(first!);
    expect(first).toHaveAttribute("aria-expanded", "true");
  });
});
