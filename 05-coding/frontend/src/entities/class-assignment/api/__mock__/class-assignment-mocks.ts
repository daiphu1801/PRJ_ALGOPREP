// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import type { AssignedProblem, AssignmentSummary } from "../../model/types";

function delay<T>(value: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

const CLASS_NAMES: Record<string, string> = {
  c1: "Lập trình Java K21",
  c2: "Cấu trúc dữ liệu K22",
  c3: "Giải thuật nâng cao K20",
};

let assignments: AssignedProblem[] = [
  {
    id: "p1",
    title: "Two Sum",
    topic: "Mảng",
    difficulty: "EASY",
    assignedClassIds: ["c1", "c2"],
    assignedClassNames: ["Lập trình Java K21", "Cấu trúc dữ liệu K22"],
    submissionCount: 512,
    acRate: 78,
  },
  {
    id: "p2",
    title: "Merge Intervals",
    topic: "Sắp xếp",
    difficulty: "MEDIUM",
    assignedClassIds: ["c1"],
    assignedClassNames: ["Lập trình Java K21"],
    submissionCount: 268,
    acRate: 54,
  },
  {
    id: "p3",
    title: "Course Schedule",
    topic: "Đồ thị",
    difficulty: "MEDIUM",
    assignedClassIds: ["c2", "c3"],
    assignedClassNames: ["Cấu trúc dữ liệu K22", "Giải thuật nâng cao K20"],
    submissionCount: 301,
    acRate: 46,
  },
  {
    id: "p4",
    title: "Coin Change",
    topic: "Quy hoạch động",
    difficulty: "HARD",
    assignedClassIds: ["c3"],
    assignedClassNames: ["Giải thuật nâng cao K20"],
    submissionCount: 189,
    acRate: 33,
  },
  {
    id: "p5",
    title: "Valid Parentheses",
    topic: "Ngăn xếp",
    difficulty: "EASY",
    assignedClassIds: ["c1", "c2", "c3"],
    assignedClassNames: [
      "Lập trình Java K21",
      "Cấu trúc dữ liệu K22",
      "Giải thuật nâng cao K20",
    ],
    submissionCount: 640,
    acRate: 82,
  },
  {
    id: "p6",
    title: "LRU Cache",
    topic: "Thiết kế",
    difficulty: "HARD",
    assignedClassIds: ["c1"],
    assignedClassNames: ["Lập trình Java K21"],
    submissionCount: 97,
    acRate: 28,
  },
];

export function listClassAssignments(): Promise<AssignedProblem[]> {
  return delay([...assignments]);
}

export function assignmentSummary(): Promise<AssignmentSummary> {
  const assignedCount = assignments.length;
  const classIds = new Set(assignments.flatMap((a) => a.assignedClassIds));
  const avgAcRate = assignments.length
    ? Math.round(
        (assignments.reduce((sum, a) => sum + a.acRate, 0) /
          assignments.length) *
          10,
      ) / 10
    : null;
  return delay({
    assignedCount,
    classCount: classIds.size,
    // Static, matching 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:261 — this mock does not
    // derive a real weekly submission trend.
    weeklySubmissionCount: 246,
    weeklySubmissionDelta: 38,
    avgAcRate,
    pendingGrading: 4,
  });
}

export function removeAssignmentFromClass(
  problemId: string,
  classId: string,
): Promise<void> {
  assignments = assignments
    .map((a) =>
      a.id === problemId
        ? {
            ...a,
            assignedClassIds: a.assignedClassIds.filter((id) => id !== classId),
            assignedClassNames: a.assignedClassIds
              .filter((id) => id !== classId)
              .map((id) => CLASS_NAMES[id] ?? id),
          }
        : a,
    )
    .filter((a) => a.assignedClassIds.length > 0);
  return delay(undefined);
}
