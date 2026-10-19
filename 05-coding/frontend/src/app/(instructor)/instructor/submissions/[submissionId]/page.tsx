"use client";

// Read-only submission result for A2 (DEC-2026-1013-instructor-avatar-menu-and-read-source).
import {
  InstructorSubmissionBreadcrumb,
  InstructorSubmissionContext,
} from "@/views/teacher/submission-context";
import { SubmissionResultView } from "@/views/users/submission-result";

export default function Page() {
  return (
    <SubmissionResultView
      area="instructor"
      renderBreadcrumb={(submission) => (
        <InstructorSubmissionBreadcrumb submission={submission} />
      )}
      renderContext={(submission) => (
        <InstructorSubmissionContext submission={submission} />
      )}
    />
  );
}
