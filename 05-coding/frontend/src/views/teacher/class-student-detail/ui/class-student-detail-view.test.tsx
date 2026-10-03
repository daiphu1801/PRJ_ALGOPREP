// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { act, fireEvent, render, renderHook, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ClassStudentDetailView } from "./class-student-detail-view";

describe("ClassStudentDetailView", () => {
  it("loads the profile of the (studentId, classId) pair passed by the caller screen", async () => {
    render(withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />));

    expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
  });

  it("shows a not-found message for a pair that does not exist", async () => {
    render(withTestProviders(<ClassStudentDetailView classId="c1" studentId="does-not-exist" />));

    expect(await screen.findByText("Không tìm thấy học viên trong lớp này")).toBeInTheDocument();
  });

  it("raises a success toast after removing the student from the class", async () => {
    const toasts = renderHook(() => useToasts());
    act(() => toast.clear());
    render(withTestProviders(<ClassStudentDetailView classId="c1" studentId="s1" />));

    fireEvent.click((await screen.findAllByRole("button", { name: "Gỡ khỏi lớp" }))[0]!);
    fireEvent.click(within(await screen.findByRole("dialog")).getByRole("button", { name: "Gỡ học viên" }));

    await waitFor(() => expect(toasts.result.current.map((item) => item.tone)).toEqual(["success"]));
    expect(toasts.result.current[0]!.message).not.toMatch(/^toast\./);
  });
});
