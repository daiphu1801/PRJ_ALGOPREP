// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
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
});
