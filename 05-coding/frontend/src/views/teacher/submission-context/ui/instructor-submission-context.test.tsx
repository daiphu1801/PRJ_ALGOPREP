// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import {
  InstructorSubmissionBreadcrumb,
  InstructorSubmissionContext,
} from "./instructor-submission-context";

let params = new URLSearchParams();
vi.mock("next/navigation", () => ({
  useSearchParams: () => params,
  useRouter: () => ({ back: vi.fn() }),
}));

const submission = {
  id: "sub-1",
  problemTitle: "Two Sum",
  submittedAt: "2026-10-10",
};

describe("instructor submission context", () => {
  beforeEach(() => {
    params = new URLSearchParams();
  });

  it("without a hint shows only the problem and the read-only note", () => {
    render(
      withTestProviders(
        <InstructorSubmissionContext submission={submission} />,
      ),
    );

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Nguyễn Văn An" }),
    ).not.toBeInTheDocument();
  });

  it("names the student and class when the hint matches a learner of that class", async () => {
    params = new URLSearchParams("studentId=s1&classId=c1&from=student");
    render(
      withTestProviders(
        <InstructorSubmissionContext submission={submission} />,
      ),
    );

    expect(
      await screen.findByRole("link", { name: "Nguyễn Văn An" }),
    ).toHaveAttribute("href", "/instructor/classes/c1/students/s1");
  });

  it("ignores a hint that names nobody of that class", async () => {
    params = new URLSearchParams("studentId=nobody&classId=c1&from=student");
    render(
      withTestProviders(
        <InstructorSubmissionContext submission={submission} />,
      ),
    );

    expect(await screen.findByText("Two Sum")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "nobody" }),
    ).not.toBeInTheDocument();
  });

  it("breadcrumb goes through class and student when opened from the student page", async () => {
    params = new URLSearchParams("studentId=s1&classId=c1&from=student");
    render(
      withTestProviders(
        <InstructorSubmissionBreadcrumb submission={submission} />,
      ),
    );

    expect(
      await screen.findByRole("link", { name: "Nguyễn Văn An" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Lập trình Java K21" }),
    ).toHaveAttribute("href", "/instructor/classes/c1");
  });

  it("breadcrumb degrades to the submission list without a hint", () => {
    params = new URLSearchParams("from=student");
    render(
      withTestProviders(
        <InstructorSubmissionBreadcrumb submission={submission} />,
      ),
    );

    expect(screen.getByRole("link", { name: "Bài nộp" })).toHaveAttribute(
      "href",
      "/instructor/submissions",
    );
  });
});
