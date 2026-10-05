// PROTOTYPE mock — no backend endpoint exists yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Data shape follows 02-bd/screens/users/USR0101_problem_list.md Sheet 5 (Khu vực A/B/D/F/G) and
// 09-layoutBase/Ngân hàng bài toán.dc.html sample rows. Deterministic — no Math.random() — so
// smoke tests and manual review see the same thing every run.
import type {
  ClassAssignmentGroup,
  InProgressProblem,
  ProblemCatalogSummary,
  ProblemDetail,
  ProblemListItem,
  ProblemListPage,
  SavedProblem,
  StarterCodeKey,
  SubmissionRunResult,
  TopicProgress,
} from "../../model/types";

const TOPICS: TopicProgress[] = [
  { id: "array", name: "Array", solved: 12, total: 30 },
  { id: "dp", name: "DP", solved: 4, total: 22 },
  { id: "graph", name: "Graph", solved: 3, total: 18 },
  { id: "tree", name: "Tree", solved: 6, total: 16 },
  { id: "string", name: "String", solved: 8, total: 14 },
  { id: "greedy", name: "Greedy", solved: 2, total: 10 },
  { id: "stack", name: "Stack", solved: 5, total: 9 },
];

const PROBLEMS: ProblemListItem[] = [
  { id: "121", code: "#121", title: "Best Time to Buy and Sell Stock", solveState: "solved", submissionModel: "both", hasSolutionReview: true, topics: ["Array"], difficulty: "EASY", acRate: 68 },
  { id: "1", code: "#1", title: "Two Sum", solveState: "solved", submissionModel: "both", hasSolutionReview: true, topics: ["Array"], difficulty: "EASY", acRate: 74 },
  { id: "139", code: "#139", title: "Word Break", solveState: "attempted", submissionModel: "both", hasSolutionReview: false, topics: ["DP", "String"], difficulty: "MEDIUM", acRate: 46 },
  { id: "200", code: "#200", title: "Number of Islands", solveState: "attempted", submissionModel: "both", hasSolutionReview: false, topics: ["Graph"], difficulty: "MEDIUM", acRate: 58 },
  { id: "207", code: "#207", title: "Course Schedule", solveState: "todo", submissionModel: "both", hasSolutionReview: false, topics: ["Graph"], difficulty: "MEDIUM", acRate: 47 },
  { id: "236", code: "#236", title: "Lowest Common Ancestor", solveState: "todo", submissionModel: "both", hasSolutionReview: false, topics: ["Tree"], difficulty: "MEDIUM", acRate: 59 },
  { id: "297", code: "#297", title: "Serialize and Deserialize Binary Tree", solveState: "todo", submissionModel: "stdioOnly", hasSolutionReview: false, topics: ["Tree"], difficulty: "HARD", acRate: 34 },
  { id: "322", code: "#322", title: "Coin Change", solveState: "solved", submissionModel: "both", hasSolutionReview: false, topics: ["DP"], difficulty: "MEDIUM", acRate: 44 },
  { id: "416", code: "#416", title: "Partition Equal Subset Sum", solveState: "todo", submissionModel: "both", hasSolutionReview: false, topics: ["DP"], difficulty: "MEDIUM", acRate: 48 },
  { id: "435", code: "#435", title: "Non-overlapping Intervals", solveState: "todo", submissionModel: "both", hasSolutionReview: false, topics: ["Greedy"], difficulty: "MEDIUM", acRate: 51 },
  { id: "704", code: "#704", title: "Binary Search", solveState: "solved", submissionModel: "both", hasSolutionReview: true, topics: ["Array"], difficulty: "EASY", acRate: 74 },
  { id: "895", code: "#895", title: "Maximum Frequency Stack", solveState: "todo", submissionModel: "stdioOnly", hasSolutionReview: false, topics: ["Stack"], difficulty: "HARD", acRate: 38 },
  { id: "1143", code: "#1143", title: "Longest Common Subsequence", solveState: "attempted", submissionModel: "both", hasSolutionReview: false, topics: ["DP"], difficulty: "MEDIUM", acRate: 57 },
  { id: "1268", code: "#1268", title: "Search Suggestions System", solveState: "todo", submissionModel: "both", hasSolutionReview: false, topics: ["String"], difficulty: "MEDIUM", acRate: null },
];

const SUMMARY: ProblemCatalogSummary = {
  solvedTotal: { solved: 42, total: 128 },
  solvedByLevel: {
    EASY: { solved: 20, total: 40 },
    MEDIUM: { solved: 18, total: 60 },
    HARD: { solved: 4, total: 28 },
  },
};

const IN_PROGRESS: InProgressProblem[] = [
  { problemId: "139", title: "Word Break", language: "python", lastVerdictLabel: "Sai kết quả 6/10" },
  { problemId: "200", title: "Number of Islands", language: "java", lastVerdictLabel: "Sai kết quả 8/12" },
  { problemId: "1143", title: "Longest Common Subsequence", language: "cpp", lastVerdictLabel: "Quá thời gian 5/20" },
];

const CLASS_ASSIGNMENTS: ClassAssignmentGroup[] = [
  {
    className: "Cấu trúc dữ liệu và giải thuật - K19",
    items: [
      { problemId: "207", title: "Course Schedule", solveState: "todo", metaLabel: "0 / 5 bài · giao 24/08" },
      { problemId: "236", title: "Lowest Common Ancestor", solveState: "todo", metaLabel: "0 / 5 bài · giao 24/08" },
    ],
  },
];

export function fetchProblemListPage(): ProblemListPage {
  return {
    summary: { ...SUMMARY },
    topics: TOPICS.map((topic) => ({ ...topic })),
    items: PROBLEMS.map((problem) => ({ ...problem })),
    inProgress: IN_PROGRESS.map((item) => ({ ...item })),
    classAssignments: CLASS_ASSIGNMENTS.map((group) => ({
      ...group,
      items: group.items.map((item) => ({ ...item })),
    })),
  };
}

const SAVED: SavedProblem[] = [
  { id: "1", code: "#1", title: "Two Sum", difficulty: "EASY", topics: ["Array"], solveState: "solved", note: "Nhớ dùng HashMap thay vì hai vòng lặp lồng nhau.", savedAtLabel: "20/09/2026" },
  { id: "139", code: "#139", title: "Word Break", difficulty: "MEDIUM", topics: ["DP", "String"], solveState: "attempted", note: "Ôn lại DP trên chuỗi trước khi thi giữa kỳ.", savedAtLabel: "18/09/2026" },
  { id: "895", code: "#895", title: "Maximum Frequency Stack", difficulty: "HARD", topics: ["Stack"], solveState: "todo", note: "", savedAtLabel: "12/09/2026" },
];

export function fetchSavedProblems(): SavedProblem[] {
  return SAVED.map((problem) => ({ ...problem }));
}

const STARTER: Record<string, string> = {
  stdio: "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // TODO: read stdin, write stdout\n    }\n}\n",
  function: "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // TODO: implement\n        return new int[0];\n    }\n}\n",
};

const PROBLEM_DETAILS: Record<string, ProblemDetail> = {
  "1": {
    id: "1",
    code: "#1",
    title: "Two Sum",
    difficulty: "EASY",
    topics: ["Array"],
    submissionModel: "both",
    statementMd:
      "Cho một mảng số nguyên `nums` và một số nguyên `target`, trả về chỉ số của hai số sao cho tổng của chúng bằng `target`.\n\nGiả định mỗi input có đúng một lời giải, và không được dùng cùng một phần tử hai lần.",
    examples: [
      { input: "nums = [2,7,11,15], target = 9", output: "[0,1]", explanation: "nums[0] + nums[1] = 2 + 7 = 9" },
      { input: "nums = [3,2,4], target = 6", output: "[1,2]" },
      { input: "nums = [3,3], target = 6", output: "[0,1]" },
    ],
    constraints: ["2 <= nums.length <= 10^4", "-10^9 <= nums[i] <= 10^9", "Chỉ có đúng một đáp án hợp lệ"],
    timeLimitMs: 1000,
    memoryLimitMb: 256,
    acRate: 74,
    starterCode: {
      "java:stdio": STARTER.stdio,
      "java:function": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[0];\n    }\n}\n",
      "cpp:stdio": "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // TODO: read stdin, write stdout\n    return 0;\n}\n",
      "cpp:function": "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {};\n    }\n};\n",
      "python:stdio": "def main():\n    # TODO: read stdin, write stdout\n    pass\n\nif __name__ == \"__main__\":\n    main()\n",
      "python:function": "class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        return []\n",
    } as Record<StarterCodeKey, string>,
    sampleTestcases: [
      { input: "[2,7,11,15]\n9", expectedOutput: "[0,1]" },
      { input: "[3,2,4]\n6", expectedOutput: "[1,2]" },
      { input: "[3,3]\n6", expectedOutput: "[0,1]" },
    ],
    mySubmissions: [
      { id: "s1", verdictLabel: "Accepted", language: "java", runtimeMs: 4, submittedAtLabel: "20/09/2026 14:02" },
      { id: "s2", verdictLabel: "Sai kết quả 2/6", language: "java", runtimeMs: null, submittedAtLabel: "20/09/2026 13:58" },
    ],
    solutionReview: {
      timeComplexity: "O(n)",
      spaceComplexity: "O(n)",
      notes: ["Dùng HashMap để tra cứu phần bù trong một lượt duyệt.", "Không có lỗi biên đáng chú ý."],
    },
  },
  "139": {
    id: "139",
    code: "#139",
    title: "Word Break",
    difficulty: "MEDIUM",
    topics: ["DP", "String"],
    submissionModel: "both",
    statementMd:
      "Cho một chuỗi `s` và một từ điển `wordDict`, trả về `true` nếu `s` có thể tách thành một dãy các từ trong từ điển.",
    examples: [{ input: 's = "leetcode", wordDict = ["leet","code"]', output: "true" }],
    constraints: ["1 <= s.length <= 300", "1 <= wordDict.length <= 1000"],
    timeLimitMs: 2000,
    memoryLimitMb: 256,
    acRate: 46,
    starterCode: {
      "java:stdio": STARTER.stdio,
      "java:function": "class Solution {\n    public boolean wordBreak(String s, List<String> wordDict) {\n        return false;\n    }\n}\n",
      "cpp:stdio": "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
      "cpp:function": "class Solution {\npublic:\n    bool wordBreak(string s, vector<string>& wordDict) {\n        return false;\n    }\n};\n",
      "python:stdio": "def main():\n    pass\n\nif __name__ == \"__main__\":\n    main()\n",
      "python:function": "class Solution:\n    def wordBreak(self, s: str, wordDict: list[str]) -> bool:\n        return False\n",
    } as Record<StarterCodeKey, string>,
    sampleTestcases: [{ input: 'leetcode\n["leet","code"]', expectedOutput: "true" }],
    mySubmissions: [
      { id: "s3", verdictLabel: "Sai kết quả 6/10", language: "python", runtimeMs: null, submittedAtLabel: "19/09/2026 09:11" },
    ],
    solutionReview: null,
  },
};

export function fetchProblemDetail(problemId: string): ProblemDetail | undefined {
  const found = PROBLEM_DETAILS[problemId];
  return found ? structuredClone(found) : undefined;
}

/**
 * ponytail: no real judge, no STOMP channel — a deterministic script the view reveals testcase by
 * testcase to preserve the "per-testcase status streams in" feel from F4-08 without wiring a fake
 * websocket for a prototype. Swap for the real channel at graduation
 * (`shared/api/stomp` per BD Sheet 4.5).
 */
export function simulateSubmission(totalCount: number): SubmissionRunResult {
  const failAt = totalCount > 4 ? 4 : totalCount; // 0-based index of the first failing case, if any
  const testcases = Array.from({ length: totalCount }, (_, index) => ({
    order: index + 1,
    verdict: (index < failAt ? "passed" : "failed") as "passed" | "failed",
  }));
  const passedCount = testcases.filter((tc) => tc.verdict === "passed").length;
  return {
    submissionId: `sub-${Date.now()}`,
    overallVerdict: passedCount === totalCount ? "accepted" : "wrongAnswer",
    passedCount,
    totalCount,
    testcases,
  };
}
