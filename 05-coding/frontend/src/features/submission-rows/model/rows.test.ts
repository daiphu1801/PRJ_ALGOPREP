import { beforeAll, describe, expect, it } from "vitest";
import type { ClassStudent } from "@/entities/class";
import type { SubmissionDetail } from "@/entities/submission";
import { renderHook, waitFor } from "@testing-library/react";
import { useInstructorReadableSubmissions } from "@/entities/submission";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { buildSubmissionRows } from "./rows";

let readable: SubmissionDetail[] = [];

beforeAll(async () => {
  const { result } = renderHook(() => useInstructorReadableSubmissions(), {
    wrapper: ({ children }) => withTestProviders(children),
  });
  await waitFor(() => expect(result.current.data).toBeDefined());
  readable = result.current.data!;
});

function student(id: string, leftAt?: string): ClassStudent {
  return {
    id,
    name: `Học viên ${id}`,
    classId: "c1",
    className: "Lập trình Java K21",
    avgScore: null,
    completionPct: 50,
    streakDays: 0,
    trend: [],
    lastActiveLabel: "-",
    status: "on_track",
    leftAt,
  };
}

// DEC-2026-1014-keep-learning-history-on-leave: a learner who left keeps their submissions in the list,
// flagged so the view can show "Đã rời lớp" and skip the student link.
describe("instructor submission rows", () => {
  it("flags the rows of a learner who left the class and keeps newest first", () => {
    const rows = buildSubmissionRows(
      [student("a"), student("b", "2026-10-05")],
      readable,
    );

    expect(rows.some((r) => r.studentId === "b" && r.leftClass)).toBe(true);
    expect(
      rows.filter((r) => r.studentId === "a").every((r) => !r.leftClass),
    ).toBe(true);
    for (let i = 1; i < rows.length; i++) {
      expect(rows[i - 1]!.submittedAt).toBeGreaterThanOrEqual(
        rows[i]!.submittedAt,
      );
    }
  });
});

describe("instructor submission rows: pass ratio", () => {
  it('shows "-" for a submission that is not judged yet and a ratio for a judged one', () => {
    const rows = buildSubmissionRows([student("a")], readable);
    for (const row of rows) {
      if (
        ["PENDING", "COMPILING", "JUDGING", "SYSTEM_ERROR"].includes(row.status)
      ) {
        expect(row.ratio).toBe("-");
      }
    }
    expect(
      rows.some((r) => r.status === "ACCEPTED" && r.ratio.includes("/")),
    ).toBe(true);
  });
});
