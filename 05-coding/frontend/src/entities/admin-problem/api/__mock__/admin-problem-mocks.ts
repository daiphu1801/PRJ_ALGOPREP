// PROTOTYPE mock — no backend endpoint exists yet.
// Rows from 09-layoutBase/Admin - Quản lý bài tập.dc.html:438-458, with ONE change: #987 was
// "Đã ẩn" there, a third lifecycle state that DEC-2026-0830-problem-lifecycle-two-states removed.
// It has real submissions, so "published" is what it becomes once only two states exist.
//
// The stat figures are adjusted to stay self-consistent with two states: the mockup shows 27 total
// / 24 published / 2 draft, which leaves one row unaccounted for — that row was the hidden one.
import type { AdminProblem, AdminProblemPage, ProblemStat } from "../../model/types";

const PROBLEMS: AdminProblem[] = [
  { code: "#121", title: "Best Time to Buy and Sell Stock", topic: "Array", difficulty: "easy", status: "published", submissionCount: 4120, acceptedRate: 68, testcaseCount: 20, editedLabel: "3 ngày trước" },
  { code: "#139", title: "Word Break", topic: "DP", difficulty: "medium", status: "published", submissionCount: 2260, acceptedRate: 46, testcaseCount: 30, editedLabel: "3 ngày trước" },
  { code: "#200", title: "Number of Islands", topic: "Graph", difficulty: "medium", status: "published", submissionCount: 2740, acceptedRate: 58, testcaseCount: 36, editedLabel: "4 ngày trước" },
  { code: "#207", title: "Course Schedule", topic: "Graph", difficulty: "medium", status: "published", submissionCount: 1980, acceptedRate: 47, testcaseCount: 32, editedLabel: "4 ngày trước" },
  { code: "#236", title: "Lowest Common Ancestor", topic: "Tree", difficulty: "medium", status: "published", submissionCount: 2320, acceptedRate: 59, testcaseCount: 24, editedLabel: "6 ngày trước" },
  { code: "#297", title: "Serialize and Deserialize Binary Tree", topic: "Tree", difficulty: "hard", status: "published", submissionCount: 860, acceptedRate: 34, testcaseCount: 40, editedLabel: "1 tuần trước" },
  { code: "#322", title: "Coin Change", topic: "DP", difficulty: "medium", status: "published", submissionCount: 2540, acceptedRate: 44, testcaseCount: 28, editedLabel: "1 tuần trước" },
  { code: "#416", title: "Partition Equal Subset Sum", topic: "DP", difficulty: "medium", status: "published", submissionCount: 1720, acceptedRate: 48, testcaseCount: 30, editedLabel: "1 tuần trước" },
  { code: "#435", title: "Non-overlapping Intervals", topic: "Greedy", difficulty: "medium", status: "published", submissionCount: 1460, acceptedRate: 51, testcaseCount: 26, editedLabel: "2 tuần trước" },
  { code: "#494", title: "Target Sum", topic: "DP", difficulty: "medium", status: "published", submissionCount: 1280, acceptedRate: 45, testcaseCount: 28, editedLabel: "2 tuần trước" },
  { code: "#543", title: "Diameter of Binary Tree", topic: "Tree", difficulty: "easy", status: "published", submissionCount: 3060, acceptedRate: 62, testcaseCount: 18, editedLabel: "2 tuần trước" },
  { code: "#621", title: "Task Scheduler", topic: "Greedy", difficulty: "medium", status: "published", submissionCount: 1340, acceptedRate: 55, testcaseCount: 24, editedLabel: "3 tuần trước" },
  { code: "#704", title: "Binary Search", topic: "Array", difficulty: "easy", status: "published", submissionCount: 4980, acceptedRate: 74, testcaseCount: 16, editedLabel: "3 tuần trước" },
  { code: "#743", title: "Network Delay Time", topic: "Graph", difficulty: "medium", status: "published", submissionCount: 940, acceptedRate: 42, testcaseCount: 34, editedLabel: "3 tuần trước" },
  { code: "#787", title: "Cheapest Flights Within K Stops", topic: "Graph", difficulty: "medium", status: "published", submissionCount: 780, acceptedRate: 37, testcaseCount: 36, editedLabel: "1 tháng trước" },
  { code: "#895", title: "Maximum Frequency Stack", topic: "Stack", difficulty: "hard", status: "published", submissionCount: 520, acceptedRate: 38, testcaseCount: 30, editedLabel: "1 tháng trước" },
  { code: "#1049", title: "Last Stone Weight II", topic: "DP", difficulty: "medium", status: "published", submissionCount: 690, acceptedRate: 51, testcaseCount: 26, editedLabel: "1 tháng trước" },
  { code: "#1143", title: "Longest Common Subsequence", topic: "DP", difficulty: "medium", status: "published", submissionCount: 1860, acceptedRate: 57, testcaseCount: 28, editedLabel: "1 tháng trước" },
  { code: "#1268", title: "Search Suggestions System", topic: "String", difficulty: "medium", status: "draft", submissionCount: 0, acceptedRate: 0, testcaseCount: 0, editedLabel: "4 giờ trước" },
  { code: "#1462", title: "Course Schedule IV", topic: "Graph", difficulty: "medium", status: "draft", submissionCount: 0, acceptedRate: 0, testcaseCount: 14, editedLabel: "2 ngày trước" },
  { code: "#987", title: "Vertical Order Traversal", topic: "Tree", difficulty: "hard", status: "published", submissionCount: 210, acceptedRate: 22, testcaseCount: 20, editedLabel: "2 tuần trước" },
];

// dc.html:477-482, "Đã xuất bản" recounted for the two-state lifecycle.
const STATS: ProblemStat[] = [
  { key: "total", value: "27", delta: "+3", deltaColorVar: "--color-success" },
  { key: "published", value: "25", delta: "93%", deltaColorVar: "--color-success" },
  { key: "draft", value: "2", delta: "+2", deltaColorVar: "--color-admin-warn" },
  { key: "acceptedRate", value: "52%", delta: "−2%", deltaColorVar: "--color-admin-warn" },
];

export function fetchAdminProblemPage(): AdminProblemPage {
  return {
    stats: STATS.map((stat) => ({ ...stat })),
    problems: PROBLEMS.map((problem) => ({ ...problem })),
    totalProblems: 27,
    publishedCount: 25,
  };
}
