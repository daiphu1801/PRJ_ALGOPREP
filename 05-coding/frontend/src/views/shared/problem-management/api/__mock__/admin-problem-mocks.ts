// PROTOTYPE mock — no backend endpoint exists yet.
// Rows from 09-layoutBase/Admin - Quản lý bài tập.dc.html:438-458, with ONE change: #987 was
// "Đã ẩn" there, a third lifecycle state that DEC-2026-0830-problem-lifecycle-two-states removed.
// It has real submissions, so "published" is what it becomes once only two states exist.
//
// The stat figures are adjusted to stay self-consistent with two states: the mockup shows 27 total
// / 24 published / 2 draft, which leaves one row unaccounted for — that row was the hidden one.
import { canViewerSeeProblem } from "@/entities/problem";
import type {
  AdminProblem,
  AdminProblemPage,
  AttentionItem,
  TopicDistributionItem,
} from "../../model/types";

const PROBLEMS: AdminProblem[] = [
  { code: "#121", title: "Best Time to Buy and Sell Stock", topic: "Array", difficulty: "EASY", status: "published", submissionCount: 4120, acceptedRate: 68, testcaseCount: 20, editedLabel: "3 ngày trước" },
  { code: "#139", title: "Word Break", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 2260, acceptedRate: 46, testcaseCount: 30, editedLabel: "3 ngày trước" },
  { code: "#200", title: "Number of Islands", topic: "Graph", difficulty: "MEDIUM", status: "published", submissionCount: 2740, acceptedRate: 58, testcaseCount: 36, editedLabel: "4 ngày trước" },
  { code: "#207", title: "Course Schedule", topic: "Graph", difficulty: "MEDIUM", status: "published", submissionCount: 1980, acceptedRate: 47, testcaseCount: 32, editedLabel: "4 ngày trước" },
  { code: "#236", title: "Lowest Common Ancestor", topic: "Tree", difficulty: "MEDIUM", status: "published", submissionCount: 2320, acceptedRate: 59, testcaseCount: 24, editedLabel: "6 ngày trước" },
  { code: "#297", title: "Serialize and Deserialize Binary Tree", topic: "Tree", difficulty: "HARD", status: "published", submissionCount: 860, acceptedRate: 34, testcaseCount: 40, editedLabel: "1 tuần trước" },
  { code: "#322", title: "Coin Change", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 2540, acceptedRate: 44, testcaseCount: 28, editedLabel: "1 tuần trước" },
  { code: "#416", title: "Partition Equal Subset Sum", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 1720, acceptedRate: 48, testcaseCount: 30, editedLabel: "1 tuần trước" },
  { code: "#435", title: "Non-overlapping Intervals", topic: "Greedy", difficulty: "MEDIUM", status: "published", submissionCount: 1460, acceptedRate: 51, testcaseCount: 26, editedLabel: "2 tuần trước" },
  { code: "#494", title: "Target Sum", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 1280, acceptedRate: 45, testcaseCount: 28, editedLabel: "2 tuần trước" },
  { code: "#543", title: "Diameter of Binary Tree", topic: "Tree", difficulty: "EASY", status: "published", submissionCount: 3060, acceptedRate: 62, testcaseCount: 18, editedLabel: "2 tuần trước" },
  { code: "#621", title: "Task Scheduler", topic: "Greedy", difficulty: "MEDIUM", status: "published", submissionCount: 1340, acceptedRate: 55, testcaseCount: 24, editedLabel: "3 tuần trước" },
  { code: "#704", title: "Binary Search", topic: "Array", difficulty: "EASY", status: "published", submissionCount: 4980, acceptedRate: 74, testcaseCount: 16, editedLabel: "3 tuần trước" },
  { code: "#743", title: "Network Delay Time", topic: "Graph", difficulty: "MEDIUM", status: "published", submissionCount: 940, acceptedRate: 42, testcaseCount: 34, editedLabel: "3 tuần trước" },
  { code: "#787", title: "Cheapest Flights Within K Stops", topic: "Graph", difficulty: "MEDIUM", status: "published", submissionCount: 780, acceptedRate: 37, testcaseCount: 36, editedLabel: "1 tháng trước" },
  { code: "#895", title: "Maximum Frequency Stack", topic: "Stack", difficulty: "HARD", status: "published", submissionCount: 520, acceptedRate: 38, testcaseCount: 30, editedLabel: "1 tháng trước" },
  { code: "#1049", title: "Last Stone Weight II", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 690, acceptedRate: 51, testcaseCount: 26, editedLabel: "1 tháng trước" },
  { code: "#1143", title: "Longest Common Subsequence", topic: "DP", difficulty: "MEDIUM", status: "published", submissionCount: 1860, acceptedRate: 57, testcaseCount: 28, editedLabel: "1 tháng trước" },
  { code: "#1268", title: "Search Suggestions System", topic: "String", difficulty: "MEDIUM", status: "draft", submissionCount: 0, acceptedRate: 0, testcaseCount: 0, editedLabel: "4 giờ trước" },
  { code: "#1462", title: "Course Schedule IV", topic: "Graph", difficulty: "MEDIUM", status: "draft", submissionCount: 0, acceptedRate: 0, testcaseCount: 14, editedLabel: "2 ngày trước" },
  { code: "#987", title: "Vertical Order Traversal", topic: "Tree", difficulty: "HARD", status: "published", submissionCount: 210, acceptedRate: 22, testcaseCount: 20, editedLabel: "2 tuần trước" },
];

// Both side blocks are derived from the rows above, never typed in: the prototype's "486 bài trên
// 12 chủ đề" contradicts its own 27-row screen (02-bd/screens/shared/SHR0201_problem_management.md
// section 4.4), so the numbers must come from the dataset actually shown.
function topicDistribution(rows: AdminProblem[]): TopicDistributionItem[] {
  const counts = new Map<string, number>();
  for (const problem of rows) counts.set(problem.topic, (counts.get(problem.topic) ?? 0) + 1);
  return [...counts.entries()]
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
    .map(([topicName, count]) => ({
      topicName,
      count,
      percent: Math.round((count / rows.length) * 100),
    }));
}

// The four auto-detected rules (BD Sheet 5 area F). The mock rows carry no publish history, so
// "hiddenFromLearners" and "staleDraft" cannot be derived and stay at 0 until the API exists.
function attention(rows: AdminProblem[]): AttentionItem[] {
  return [
    { ruleCode: "noTestcase", count: rows.filter((problem) => problem.testcaseCount === 0).length },
    {
      ruleCode: "lowAcceptRate",
      count: rows.filter(
        (problem) => problem.status === "published" && problem.acceptedRate < 30,
      ).length,
    },
    { ruleCode: "hiddenFromLearners", count: 0 },
    { ruleCode: "staleDraft", count: 0 },
  ];
}

/**
 * `ListProblemsAdmin`. A2 only gets the problems they wrote (BD SHR0201 Q1); the blocks and totals
 * are derived from the rows returned, so A2 never sees counts that include other people's work. A3
 * keeps the mockup's own totals (27 / 25), which the 21 sample rows cannot reproduce.
 */
export async function loadAdminProblemPage(): Promise<AdminProblemPage> {
  await new Promise((resolve) => setTimeout(resolve, 150));
  const rows = PROBLEMS.filter((problem) => canViewerSeeProblem(problem.code)).map((problem) => ({ ...problem }));
  const scoped = rows.length !== PROBLEMS.length;
  return {
    topicDistribution: topicDistribution(rows),
    attention: attention(rows),
    problems: rows,
    totalProblems: scoped ? rows.length : 27,
    publishedCount: scoped ? rows.filter((problem) => problem.status === "published").length : 25,
  };
}
