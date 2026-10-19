// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassStudentDetailView } from "./class-student-detail-view";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ back: vi.fn(), push: vi.fn() }),
}));

describe("ClassStudentDetailView", () => {
  it("loads the profile of the (studentId, classId) pair passed by the caller screen", async () => {
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    expect(
      (await screen.findAllByText("Nguyễn Văn An")).length,
    ).toBeGreaterThan(0);
    // Contact details the instructor needs to reach the student; no stat strip, no "last active".
    expect(
      screen.getByRole("link", {
        name: "an.nguyenvan@student.algoprep.edu.vn",
      }),
    ).toHaveAttribute("href", "mailto:an.nguyenvan@student.algoprep.edu.vn");
    expect(screen.getByText("Số điện thoại")).toBeInTheDocument();
    expect(screen.queryByText("Chuỗi ngày")).not.toBeInTheDocument();
    expect(screen.queryByText("Hoạt động gần nhất")).not.toBeInTheDocument();
  });

  it("lists the submission history with a verdict badge and a read-only link that carries the student (INS0204)", async () => {
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    const history = await screen.findByRole("table", {
      name: "Lịch sử nộp bài",
    });
    // Verdicts are the learner-facing labels, not the raw enum codes.
    expect(within(history).getByText("Đã chấp nhận")).toBeInTheDocument();
    expect(within(history).getByText("Sai kết quả")).toBeInTheDocument();
    expect(within(history).getByText("Lỗi biên dịch")).toBeInTheDocument();
    expect(within(history).queryByText("WRONG_ANSWER")).not.toBeInTheDocument();
    // Language and ratio come from the same submission the link opens (SB-90412: Python, 24/24).
    expect(within(history).getByText("24/24")).toBeInTheDocument();
    expect(within(history).getAllByText("Python")).toHaveLength(2);
    expect(
      within(history).getByRole("link", { name: "Xem kết quả bài Two Sum" }),
    ).toHaveAttribute(
      "href",
      "/instructor/submissions/SB-90412?studentId=s1&classId=c1&from=student",
    );
  });

  it("shows a not-found message for a pair that does not exist", async () => {
    render(
      withTestProviders(
        <ClassStudentDetailView classId="c1" studentId="does-not-exist" />,
      ),
    );

    expect(
      await screen.findByText("Không tìm thấy học viên trong lớp này"),
    ).toBeInTheDocument();
  });

  it("raises a success toast after removing the student from the class", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(
      withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />),
    );

    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Gỡ khỏi lớp" }))[0]!,
    );
    fireEvent.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Gỡ học viên",
      }),
    );

    await waitFor(() =>
      expect(toasts.result.current.map((item) => item.tone)).toEqual([
        "success",
      ]),
    );
    expect(toasts.result.current[0]!.message).not.toMatch(/^toast\./);
  });
});
