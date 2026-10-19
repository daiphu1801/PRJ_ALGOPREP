// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import type {
  AssignableProblem,
  AssignedProblem,
  AssignmentSummary,
} from "../../model/types";

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
    id: "1",
    title: "Two Sum",
    topic: "Mảng",
    topics: ["Mảng", "Bảng băm"],
    difficulty: "EASY",
    assignedClassIds: ["c1", "c2"],
    assignedClassNames: ["Lập trình Java K21", "Cấu trúc dữ liệu K22"],
    submissionCount: 512,
    acRate: 78,
  },
  {
    id: "56",
    title: "Merge Intervals",
    topic: "Sắp xếp",
    difficulty: "MEDIUM",
    assignedClassIds: ["c1"],
    assignedClassNames: ["Lập trình Java K21"],
    submissionCount: 268,
    acRate: 54,
  },
  {
    id: "207",
    title: "Course Schedule",
    topic: "Đồ thị",
    topics: ["Đồ thị", "Sắp xếp tô pô", "Duyệt BFS"],
    difficulty: "MEDIUM",
    assignedClassIds: ["c2", "c3"],
    assignedClassNames: ["Cấu trúc dữ liệu K22", "Giải thuật nâng cao K20"],
    submissionCount: 301,
    acRate: 46,
  },
  {
    id: "322",
    title: "Coin Change",
    topic: "Quy hoạch động",
    topics: ["Quy hoạch động", "Đệ quy"],
    difficulty: "HARD",
    assignedClassIds: ["c3"],
    assignedClassNames: ["Giải thuật nâng cao K20"],
    submissionCount: 189,
    acRate: 33,
  },
  {
    id: "20",
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
    id: "146",
    title: "LRU Cache",
    topic: "Thiết kế",
    difficulty: "HARD",
    assignedClassIds: ["c1"],
    assignedClassNames: ["Lập trình Java K21"],
    submissionCount: 97,
    acRate: 28,
    withdrawn: true,
  },
];

/**
 * Mock only: split a problem's total over its classes (weights 1, 2, 3 ... by position) so the class
 * tab has figures that add up to the total; the real endpoint aggregates per class and skips learners
 * who left (left_at IS NULL).
 */
function splitByClass(a: AssignedProblem): AssignedProblem["classStats"] {
  const weights = a.assignedClassIds.map((_, i) => i + 1);
  const sum = weights.reduce((x, y) => x + y, 0);
  let left = a.submissionCount;
  return Object.fromEntries(
    a.assignedClassIds.map((id, i) => {
      const count =
        i === a.assignedClassIds.length - 1
          ? left
          : Math.round((a.submissionCount * weights[i]!) / sum);
      left -= count;
      return [
        id,
        {
          submissionCount: count,
          acRate: Math.min(99, Math.max(1, a.acRate + (i % 2 === 0 ? 4 : -3))),
        },
      ];
    }),
  );
}

assignments = assignments.map((a) => ({ ...a, classStats: splitByClass(a) }));

export function listClassAssignments(): Promise<AssignedProblem[]> {
  return delay([...assignments]);
}

export function assignmentSummary(): Promise<AssignmentSummary> {
  const assignedCount = assignments.length;
  const classIds = new Set(assignments.flatMap((a) => a.assignedClassIds));
  return delay({ assignedCount, classCount: classIds.size });
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
            classStats: Object.fromEntries(
              Object.entries(a.classStats ?? {}).filter(
                ([id]) => id !== classId,
              ),
            ),
          }
        : a,
    )
    .filter((a) => a.assignedClassIds.length > 0);
  return delay(undefined);
}

// Published bank problems no class has yet. Problems already in `assignments` are offered too, for
// the classes that do not have them.
const EXTRA_BANK: AssignableProblem[] = [
  { id: "704", title: "Binary Search", topic: "Mảng", difficulty: "EASY" },
  {
    id: "200",
    title: "Number of Islands",
    topic: "Đồ thị",
    difficulty: "MEDIUM",
  },
  {
    id: "139",
    title: "Word Break",
    topic: "Quy hoạch động",
    difficulty: "MEDIUM",
  },
  {
    id: "1143",
    title: "Longest Common Subsequence",
    topic: "Quy hoạch động",
    difficulty: "HARD",
  },
];

export function listAssignableProblems(
  classId: string,
): Promise<AssignableProblem[]> {
  const knownIds = new Set(assignments.map((a) => a.id));
  return delay([
    ...assignments
      .filter((a) => !a.assignedClassIds.includes(classId))
      .map(({ id, title, topic, difficulty }) => ({
        id,
        title,
        topic,
        difficulty,
      })),
    ...EXTRA_BANK.filter((b) => !knownIds.has(b.id)),
  ]);
}

export function assignProblemsToClass(
  classId: string,
  problemIds: string[],
): Promise<void> {
  const className = CLASS_NAMES[classId] ?? classId;
  for (const id of problemIds) {
    const existing = assignments.find((a) => a.id === id);
    if (existing) {
      if (!existing.assignedClassIds.includes(classId)) {
        existing.assignedClassIds = [...existing.assignedClassIds, classId];
        existing.assignedClassNames = [
          ...existing.assignedClassNames,
          className,
        ];
      }
      continue;
    }
    const bank = EXTRA_BANK.find((b) => b.id === id);
    if (!bank) continue;
    assignments = [
      ...assignments,
      {
        ...bank,
        assignedClassIds: [classId],
        assignedClassNames: [className],
        submissionCount: 0,
        acRate: 0,
      },
    ];
  }
  return delay(undefined);
}
