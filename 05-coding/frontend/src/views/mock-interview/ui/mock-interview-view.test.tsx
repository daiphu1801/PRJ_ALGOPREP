// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// BD `02-bd/screens/users/USR0302_mock_interview.md` V0.2 fixed a real defect: the rubric used to
// carry the WRONG 4 labels (Kỹ thuật/Giao tiếp/Giải quyết vấn đề/Chất lượng mã). The correct set is
// Độ rõ ràng/Độ chính xác kỹ thuật/Khả năng phản biện/Nhận thức độ phức tạp at 25/30/25/20%. This
// test pins the correct labels and asserts the old ones never render again.
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { MockInterviewView } from "./mock-interview-view";

function renderView() {
  return render(withTestProviders(<MockInterviewView />));
}

describe("MockInterviewView", () => {
  it("renders the 3 entry tabs and the 4-level session config", () => {
    renderView();

    expect(screen.getByRole("button", { name: "Bài nộp Accepted" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Kho câu hỏi" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tự chọn chủ đề" })).toBeInTheDocument();
    for (const level of ["Intern", "Junior", "Middle", "Senior"]) {
      expect(screen.getByRole("button", { name: level })).toBeInTheDocument();
    }
  });

  it("keeps the Start button disabled until a submission is picked", () => {
    renderView();

    expect(screen.getByRole("button", { name: "Bắt đầu phiên" })).toBeDisabled();
    fireEvent.click(screen.getByText("1. Two Sum"));
    expect(screen.getByRole("button", { name: "Bắt đầu phiên" })).not.toBeDisabled();
  });

  it("shows exactly the correct 4 rubric labels at result — never the old wrong set", async () => {
    renderView();

    fireEvent.click(screen.getByText("1. Two Sum"));
    fireEvent.click(screen.getByRole("button", { name: "Bắt đầu phiên" }));

    // Ending early (H popup) skips straight to the result screen without playing out 12 turns.
    fireEvent.click(await screen.findByRole("button", { name: "Kết thúc sớm" }));
    fireEvent.click(screen.getByRole("button", { name: "Kết thúc" }));

    expect(await screen.findByText(/Độ rõ ràng/)).toBeInTheDocument();
    expect(screen.getByText(/Độ chính xác kỹ thuật/)).toBeInTheDocument();
    expect(screen.getByText(/Khả năng phản biện/)).toBeInTheDocument();
    expect(screen.getByText(/Nhận thức độ phức tạp/)).toBeInTheDocument();

    for (const wrongLabel of ["Kỹ thuật", "Giao tiếp", "Giải quyết vấn đề", "Chất lượng mã"]) {
      expect(screen.queryByText(wrongLabel)).not.toBeInTheDocument();
    }
  });
});
