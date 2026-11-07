// PROTOTYPE mock — no backend endpoint exists yet.
// Rows from 09-layoutBase/Admin - Quản lý bài tập.dc.html:438-458, with ONE change: #987 was
// "Đã ẩn" there, a third lifecycle state that DEC-2026-0830-problem-lifecycle-two-states removed.
// It has real submissions, so "published" is what it becomes once only two states exist.
//
// The stat figures are adjusted to stay self-consistent with two states: the mockup shows 27 total
// / 24 published / 2 draft, which leaves one row unaccounted for — that row was the hidden one.
import {
  canViewerSeeProblem,
  getProblemLevels,
  listCreatedProblems,
  problemLevelRank,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import type {
  AdminListParams,
  AdminProblem,
  AdminProblemList,
  AttentionItem,
  BulkResult,
  ProblemStatus,
  TopicDistribution,
} from "../../model/types";

/** The shape the literal below is written in; `toRow` turns it into what the screen reads. */
type RawProblem = {
  code: string;
  title: string;
  topic: string;
  difficulty: string;
  status: ProblemStatus;
  submissionCount: number;
  acceptedRate: number;
  testcaseCount: number;
  editedLabel: string;
};

// Classes each problem is currently assigned to; absent = 0. The real value comes from
// class_assignments rows with removed_at IS NULL (BD SHR0201 Sheet 7, DeleteProblem).
const ASSIGNED_CLASSES: Record<string, number> = {
  "#121": 1,
  "#139": 2,
  "#200": 1,
  "#322": 3,
  "#704": 2,
};

const PROBLEMS: RawProblem[] = [
  {
    code: "#121",
    title: "Best Time to Buy and Sell Stock",
    topic: "Array",
    difficulty: "EASY",
    status: "published",
    submissionCount: 4120,
    acceptedRate: 68,
    testcaseCount: 20,
    editedLabel: "3 ngày trước",
  },
  {
    code: "#139",
    title: "Word Break",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 2260,
    acceptedRate: 46,
    testcaseCount: 30,
    editedLabel: "3 ngày trước",
  },
  {
    code: "#200",
    title: "Number of Islands",
    topic: "Graph",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 2740,
    acceptedRate: 58,
    testcaseCount: 36,
    editedLabel: "4 ngày trước",
  },
  {
    code: "#207",
    title: "Course Schedule",
    topic: "Graph",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1980,
    acceptedRate: 47,
    testcaseCount: 32,
    editedLabel: "4 ngày trước",
  },
  {
    code: "#236",
    title: "Lowest Common Ancestor",
    topic: "Tree",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 2320,
    acceptedRate: 59,
    testcaseCount: 24,
    editedLabel: "6 ngày trước",
  },
  {
    code: "#297",
    title: "Serialize and Deserialize Binary Tree",
    topic: "Tree",
    difficulty: "HARD",
    status: "published",
    submissionCount: 860,
    acceptedRate: 34,
    testcaseCount: 40,
    editedLabel: "1 tuần trước",
  },
  {
    code: "#322",
    title: "Coin Change",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 2540,
    acceptedRate: 44,
    testcaseCount: 28,
    editedLabel: "1 tuần trước",
  },
  {
    code: "#416",
    title: "Partition Equal Subset Sum",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1720,
    acceptedRate: 48,
    testcaseCount: 30,
    editedLabel: "1 tuần trước",
  },
  {
    code: "#435",
    title: "Non-overlapping Intervals",
    topic: "Greedy",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1460,
    acceptedRate: 51,
    testcaseCount: 26,
    editedLabel: "2 tuần trước",
  },
  {
    code: "#494",
    title: "Target Sum",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1280,
    acceptedRate: 45,
    testcaseCount: 28,
    editedLabel: "2 tuần trước",
  },
  {
    code: "#543",
    title: "Diameter of Binary Tree",
    topic: "Tree",
    difficulty: "EASY",
    status: "published",
    submissionCount: 3060,
    acceptedRate: 62,
    testcaseCount: 18,
    editedLabel: "2 tuần trước",
  },
  {
    code: "#621",
    title: "Task Scheduler",
    topic: "Greedy",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1340,
    acceptedRate: 55,
    testcaseCount: 24,
    editedLabel: "3 tuần trước",
  },
  {
    code: "#704",
    title: "Binary Search",
    topic: "Array",
    difficulty: "EASY",
    status: "published",
    submissionCount: 4980,
    acceptedRate: 74,
    testcaseCount: 16,
    editedLabel: "3 tuần trước",
  },
  {
    code: "#743",
    title: "Network Delay Time",
    topic: "Graph",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 940,
    acceptedRate: 42,
    testcaseCount: 34,
    editedLabel: "3 tuần trước",
  },
  {
    code: "#787",
    title: "Cheapest Flights Within K Stops",
    topic: "Graph",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 780,
    acceptedRate: 37,
    testcaseCount: 36,
    editedLabel: "1 tháng trước",
  },
  {
    code: "#895",
    title: "Maximum Frequency Stack",
    topic: "Stack",
    difficulty: "HARD",
    status: "published",
    submissionCount: 520,
    acceptedRate: 38,
    testcaseCount: 30,
    editedLabel: "1 tháng trước",
  },
  {
    code: "#1049",
    title: "Last Stone Weight II",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 690,
    acceptedRate: 51,
    testcaseCount: 26,
    editedLabel: "1 tháng trước",
  },
  {
    code: "#1143",
    title: "Longest Common Subsequence",
    topic: "DP",
    difficulty: "MEDIUM",
    status: "published",
    submissionCount: 1860,
    acceptedRate: 57,
    testcaseCount: 28,
    editedLabel: "1 tháng trước",
  },
  {
    code: "#1268",
    title: "Search Suggestions System",
    topic: "String",
    difficulty: "MEDIUM",
    status: "draft",
    submissionCount: 0,
    acceptedRate: 0,
    testcaseCount: 0,
    editedLabel: "4 giờ trước",
  },
  {
    code: "#1462",
    title: "Course Schedule IV",
    topic: "Graph",
    difficulty: "MEDIUM",
    status: "draft",
    submissionCount: 0,
    acceptedRate: 0,
    testcaseCount: 14,
    editedLabel: "2 ngày trước",
  },
  {
    code: "#987",
    title: "Vertical Order Traversal",
    topic: "Tree",
    difficulty: "HARD",
    status: "published",
    submissionCount: 210,
    acceptedRate: 22,
    testcaseCount: 20,
    editedLabel: "2 tuần trước",
  },
];

const UNITS_MS: Record<string, number> = {
  giờ: 3_600_000,
  ngày: 86_400_000,
  tuần: 7 * 86_400_000,
  tháng: 30 * 86_400_000,
};

/** "3 ngày trước" in the literal above becomes a timestamp, so the screen prints it the way it prints a real one. */
function ageToIso(label: string): string {
  const match = /(\d+)\s*(giờ|ngày|tuần|tháng)/.exec(label);
  const age = match ? Number(match[1]) * UNITS_MS[match[2]!]! : 0;
  return new Date(Date.now() - age).toISOString();
}

// Changes made through the bulk actions. The literal rows stay read-only; these overlay them, which is
// all a prototype needs to show a delete, a publish or a level change taking effect. A page reload resets them.
const deleted = new Set<string>();
const levelOf = new Map<string, string>();
const statusOf = new Map<string, ProblemStatus>();

/** Test hook: forget every overlay change. */
export function resetAdminProblemMock(): void {
  deleted.clear();
  levelOf.clear();
  statusOf.clear();
}

function toRow(problem: RawProblem): AdminProblem {
  const id = problem.code.replace("#", "");
  return {
    id,
    code: problem.code,
    title: problem.title,
    topics: [problem.topic],
    difficulty: levelOf.get(id) ?? problem.difficulty,
    status: statusOf.get(id) ?? problem.status,
    submissionCount: problem.submissionCount,
    assignedClassCount: ASSIGNED_CLASSES[problem.code] ?? 0,
    acceptedRate: problem.acceptedRate,
    testcaseCount: problem.testcaseCount,
    updatedAt: ageToIso(problem.editedLabel),
  };
}

/**
 * Every row the current mock viewer may see, before filters. A2 only gets the problems they wrote (BD
 * SHR0201 Q1); the blocks and totals derive from these rows, so A2 never sees counts that include other
 * people's work. Problems created in this session (new form or duplicate) come first, newest on top.
 */
function visibleRows(): AdminProblem[] {
  const fresh = listCreatedProblems()
    .filter((problem) => canViewerSeeProblem(problem.code))
    .map((problem): RawProblem => ({
      ...problem,
      submissionCount: 0,
      acceptedRate: 0,
      editedLabel: "Vừa xong",
    }))
    .reverse();
  const own = PROBLEMS.filter((problem) => canViewerSeeProblem(problem.code));
  return [...fresh, ...own]
    .map(toRow)
    .filter((problem) => !deleted.has(problem.id));
}

const DELAY_MS = 150;
const pause = () => new Promise((resolve) => setTimeout(resolve, DELAY_MS));

function compare(
  left: AdminProblem,
  right: AdminProblem,
  key: AdminListParams["sort"],
): number {
  switch (key) {
    case "submissions":
      return left.submissionCount - right.submissionCount;
    case "accepted":
      return (left.acceptedRate ?? -1) - (right.acceptedRate ?? -1);
    case "difficulty": {
      // Admin-chosen order of the level list, not a fixed easy < medium < hard.
      const levels = getProblemLevels();
      return (
        problemLevelRank(levels, left.difficulty) -
        problemLevelRank(levels, right.difficulty)
      );
    }
    case "status":
      return left.status.localeCompare(right.status);
    case "code":
      return Number(left.id) - Number(right.id);
    case "edited":
      return left.updatedAt.localeCompare(right.updatedAt);
    default:
      return left.title.localeCompare(right.title, "vi");
  }
}

/**
 * `ListProblemsAdmin` with the same contract as the real endpoint: filter, sort and page on the
 * "server", a `page` past the end answers the last page, totals ignore the filter.
 */
export async function loadAdminProblemList(
  params: AdminListParams,
): Promise<AdminProblemList> {
  await pause();
  const all = visibleRows();
  const needle = params.q.trim().toLowerCase();
  const rows = all
    .filter(
      (problem) =>
        (params.level === "all" || problem.difficulty === params.level) &&
        (params.status === "all" || problem.status === params.status) &&
        (!needle ||
          problem.code.toLowerCase().includes(needle) ||
          problem.title.toLowerCase().includes(needle)),
    )
    .sort(
      (left, right) =>
        compare(left, right, params.sort) * (params.dir === "asc" ? 1 : -1),
    );
  const totalPages = Math.max(1, Math.ceil(rows.length / params.pageSize));
  const number = Math.min(Math.max(1, params.page), totalPages);
  const levels: Record<string, number> = {};
  const topics: Record<string, number> = {};
  for (const problem of all) {
    levels[problem.difficulty] = (levels[problem.difficulty] ?? 0) + 1;
    for (const topic of problem.topics)
      topics[topic] = (topics[topic] ?? 0) + 1;
  }
  return {
    items: rows.slice((number - 1) * params.pageSize, number * params.pageSize),
    page: {
      number,
      size: params.pageSize,
      totalItems: rows.length,
      totalPages,
    },
    totals: {
      total: all.length,
      published: all.filter((problem) => problem.status === "published").length,
    },
    usage: { levels, topics },
  };
}

// Both side blocks are derived from the rows above, never typed in: the prototype's "486 bài trên
// 12 chủ đề" contradicts its own 27-row screen (02-bd/screens/shared/SHR0201_problem_management.md
// section 4.4), so the numbers must come from the dataset actually shown.
export async function loadTopicDistribution(): Promise<TopicDistribution> {
  await pause();
  const rows = visibleRows();
  const counts = new Map<string, number>();
  for (const problem of rows)
    for (const topic of problem.topics)
      counts.set(topic, (counts.get(topic) ?? 0) + 1);
  return {
    totalProblemsWithTopic: rows.length,
    totalTopicsUsed: counts.size,
    items: [...counts.entries()]
      .sort(
        (left, right) => right[1] - left[1] || left[0].localeCompare(right[0]),
      )
      .map(([topicName, count]) => ({
        topicName,
        count,
        percent: Math.round((count / rows.length) * 100),
      })),
  };
}

// The four auto-detected rules (BD Sheet 5 area F). The mock rows carry no publish history, so
// "hiddenFromLearners" and "staleDraft" cannot be derived and stay at 0 until the API exists.
export async function loadAttentionList(): Promise<AttentionItem[]> {
  await pause();
  const rows = visibleRows();
  return [
    {
      ruleCode: "noTestcase",
      count: rows.filter((problem) => problem.testcaseCount === 0).length,
    },
    {
      ruleCode: "lowAcceptRate",
      count: rows.filter(
        (problem) =>
          problem.status === "published" &&
          problem.acceptedRate !== null &&
          problem.acceptedRate < 30,
      ).length,
    },
    { ruleCode: "hiddenFromLearners", count: 0 },
    { ruleCode: "staleDraft", count: 0 },
  ];
}

const PUBLISH_MAX = 20;

/** Each id is applied on its own; one that is not in the viewer's rows fails alone (PRB-010). */
async function applyBulk(
  ids: string[],
  change: (id: string) => void,
): Promise<BulkResult> {
  await pause();
  const known = new Set(visibleRows().map((problem) => problem.id));
  const result: BulkResult = { succeededIds: [], failedItems: [] };
  for (const id of new Set(ids)) {
    if (known.has(id)) {
      change(id);
      result.succeededIds.push(id);
    } else result.failedItems.push({ id, code: "PRB-010" });
  }
  return result;
}

export function mockBulkPublish(ids: string[]): Promise<BulkResult> {
  if (ids.length > PUBLISH_MAX)
    return Promise.reject(new ApiError("PRB-001", 400));
  return applyBulk(ids, (id) => statusOf.set(id, "published"));
}

export const mockBulkDelete = (ids: string[]) =>
  applyBulk(ids, (id) => deleted.add(id));

export const mockBulkDifficulty = (ids: string[], levelKey: string) =>
  applyBulk(ids, (id) => levelOf.set(id, levelKey));

/** A small CSV of the chosen rows, so the mock download has the same shape as the file the server sends. */
export async function mockExportProblems(
  ids: string[],
): Promise<{ blob: Blob; filename: string | null }> {
  await pause();
  const chosen = new Set(ids);
  const lines = visibleRows()
    .filter((problem) => chosen.has(problem.id))
    .map((problem) =>
      [
        problem.code,
        `"${problem.title.replaceAll('"', '""')}"`,
        `"${problem.topics.join("; ")}"`,
        problem.difficulty,
        problem.status,
        problem.submissionCount,
        problem.acceptedRate ?? "",
        problem.testcaseCount,
      ].join(","),
    );
  const header =
    "Mã,Tiêu đề,Chủ đề,Độ khó,Trạng thái,Lượt nộp,Tỉ lệ AC,Số testcase";
  return {
    blob: new Blob(["\uFEFF" + [header, ...lines].join("\r\n")], {
      type: "text/csv;charset=utf-8",
    }),
    filename: "problems-mock.csv",
  };
}
