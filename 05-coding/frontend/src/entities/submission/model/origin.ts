// Where the instructor opened the read-only submission result from (DEC-2026-1014-instructor-submissions-list-details):
// the query parameter `from=submissions|student|class` only drives the breadcrumb and the lit sidebar item.
// A missing or unknown value is the submission list.
export type SubmissionOrigin = "submissions" | "student" | "class";

export function parseSubmissionOrigin(
  value: string | null | undefined,
): SubmissionOrigin {
  return value === "student" || value === "class" ? value : "submissions";
}

/** The instructor sidebar item of each calling screen, kept lit on the result page. */
export const SUBMISSION_ORIGIN_NAV_HREF: Record<SubmissionOrigin, string> = {
  submissions: "/instructor/submissions",
  student: "/instructor/students",
  class: "/instructor/classes",
};
