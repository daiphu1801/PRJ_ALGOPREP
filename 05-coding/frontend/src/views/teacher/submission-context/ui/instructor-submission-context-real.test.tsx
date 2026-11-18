// The instructor context card in REAL mode (F4 14.3): a learner who left the class keeps their name,
// picture and class name (the work is theirs) but nothing links to a page they can no longer open.
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ back: vi.fn() }),
}));

// Import after the mocks.
import { InstructorSubmissionContext } from "./instructor-submission-context";

const m = viMessages.instructorSubmission;

function submission(leftClass: boolean) {
  return {
    id: "sub-1",
    problemTitle: "Two Sum",
    submittedAt: "2026-10-10",
    instructorContext: {
      studentId: "s1",
      studentName: "Trần Bình",
      studentAvatarVersion: null,
      classId: "c1",
      className: "Lớp A",
      leftClass,
    },
  };
}

describe("InstructorSubmissionContext with the real API", () => {
  it("links the student and the class and shows no badge while the student is in the class", () => {
    render(
      withTestProviders(
        <InstructorSubmissionContext submission={submission(false)} />,
      ),
    );

    expect(screen.getByRole("link", { name: "Trần Bình" })).toHaveAttribute(
      "href",
      "/instructor/classes/c1/students/s1",
    );
    expect(screen.getByRole("link", { name: "Lớp A" })).toBeInTheDocument();
    expect(screen.queryByText(m.leftClassBadge)).not.toBeInTheDocument();
  });

  it("keeps the name and picture of a student who left, adds the badge and drops the links", () => {
    render(
      withTestProviders(
        <InstructorSubmissionContext submission={submission(true)} />,
      ),
    );

    expect(screen.getByText("Trần Bình")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Trần Bình" })).toBeInTheDocument();
    expect(screen.getByText("Lớp A")).toBeInTheDocument();
    expect(screen.getByText(m.leftClassBadge)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Trần Bình" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Lớp A" })).toBeNull();
  });
});
