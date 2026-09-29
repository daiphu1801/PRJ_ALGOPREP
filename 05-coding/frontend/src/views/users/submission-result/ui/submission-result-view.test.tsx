import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { SubmissionResultView } from "./submission-result-view";

let currentSubmissionId = "SB-90412";
vi.mock("next/navigation", () => ({
  useParams: () => ({ submissionId: currentSubmissionId }),
}));

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <SubmissionResultView />
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
    expect(screen.getByText(/expected ';' after expression/)).toBeInTheDocument();
  });

  it("shows a not-found state for an unknown submission id", () => {
    currentSubmissionId = "does-not-exist";
    renderView();

    expect(screen.getByText("Không tìm thấy bài nộp.")).toBeInTheDocument();
  });
});
