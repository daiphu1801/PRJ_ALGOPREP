// PROTOTYPE mock — no backend endpoint exists yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Data shape follows 02-bd/screens/users/USR0101_problem_list.md Sheet 5 (Khu vực A/B/D/F/G) and
// 09-layoutBase/Ngân hàng bài toán.dc.html sample rows. Deterministic — no Math.random() — so
// smoke tests and manual review see the same thing every run.
import type {
  ClassAssignmentGroup,
  InProgressProblem,
  ProblemDetail,
  ProblemListItem,
  ProblemListPage,
  SampleRunResult,
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
  {
    id: "121",
    code: "#121",
    title: "Best Time to Buy and Sell Stock",
    solveState: "solved",
    submissionModel: "both",
    hasSolutionReview: true,
    topics: ["Array"],
    difficulty: "EASY",
    acRate: 68,
  },
  {
    id: "1",
    code: "#1",
    title: "Two Sum",
    solveState: "solved",
    submissionModel: "both",
    hasSolutionReview: true,
    topics: ["Array"],
    difficulty: "EASY",
    acRate: 74,
  },
  {
    id: "139",
    code: "#139",
    title: "Word Break",
    solveState: "attempted",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["DP", "String"],
    difficulty: "MEDIUM",
    acRate: 46,
  },
  {
    id: "200",
    code: "#200",
    title: "Number of Islands",
    solveState: "attempted",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["Graph"],
    difficulty: "MEDIUM",
    acRate: 58,
  },
  {
    id: "207",
    code: "#207",
    title: "Course Schedule",
    solveState: "todo",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["Graph"],
    difficulty: "MEDIUM",
    acRate: 47,
  },
  {
    id: "236",
    code: "#236",
    title: "Lowest Common Ancestor",
    solveState: "todo",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["Tree"],
    difficulty: "MEDIUM",
    acRate: 59,
  },
  {
    id: "297",
    code: "#297",
    title: "Serialize and Deserialize Binary Tree",
    solveState: "todo",
    submissionModel: "stdioOnly",
    hasSolutionReview: false,
    topics: ["Tree"],
    difficulty: "HARD",
    acRate: 34,
  },
  {
    id: "322",
    code: "#322",
    title: "Coin Change",
    solveState: "solved",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["DP"],
    difficulty: "MEDIUM",
    acRate: 44,
  },
  {
    id: "416",
    code: "#416",
    title: "Partition Equal Subset Sum",
    solveState: "todo",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["DP"],
    difficulty: "MEDIUM",
    acRate: 48,
  },
  {
    id: "435",
    code: "#435",
    title: "Non-overlapping Intervals",
    solveState: "todo",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["Greedy"],
    difficulty: "MEDIUM",
    acRate: 51,
  },
  {
    id: "704",
    code: "#704",
    title: "Binary Search",
    solveState: "solved",
    submissionModel: "both",
    hasSolutionReview: true,
    topics: ["Array"],
    difficulty: "EASY",
    acRate: 74,
  },
  {
    id: "895",
    code: "#895",
    title: "Maximum Frequency Stack",
    solveState: "todo",
    submissionModel: "stdioOnly",
    hasSolutionReview: false,
    topics: ["Stack"],
    difficulty: "HARD",
    acRate: 38,
  },
  {
    id: "1143",
    code: "#1143",
    title: "Longest Common Subsequence",
    solveState: "attempted",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["DP"],
    difficulty: "MEDIUM",
    acRate: 57,
  },
  {
    id: "1268",
    code: "#1268",
    title: "Search Suggestions System",
    solveState: "todo",
    submissionModel: "both",
    hasSolutionReview: false,
    topics: ["String"],
    difficulty: "MEDIUM",
    acRate: null,
  },
];

const IN_PROGRESS: InProgressProblem[] = [
  {
    problemId: "139",
    title: "Word Break",
    language: "python",
    lastVerdict: "wrongAnswer",
    passedCount: 6,
    totalCount: 10,
  },
  {
    problemId: "200",
    title: "Number of Islands",
    language: "java",
    lastVerdict: "wrongAnswer",
    passedCount: 8,
    totalCount: 12,
  },
  {
    problemId: "1143",
    title: "Longest Common Subsequence",
    language: "cpp",
    lastVerdict: "timeLimitExceeded",
    passedCount: 5,
    totalCount: 20,
  },
];

const CLASS_ASSIGNMENTS: ClassAssignmentGroup[] = [
  {
    className: "Cấu trúc dữ liệu và giải thuật - K19",
    items: [
      {
        problemId: "207",
        title: "Course Schedule",
        solveState: "todo",
        solvedCount: 0,
        totalCount: 5,
        assignedAt: "2026-08-24T02:00:00Z",
      },
      {
        problemId: "236",
        title: "Lowest Common Ancestor",
        solveState: "todo",
        solvedCount: 0,
        totalCount: 5,
        assignedAt: "2026-08-24T02:00:00Z",
      },
    ],
  },
];

export function fetchProblemListPage(): ProblemListPage {
  return {
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
  {
    id: "1",
    code: "#1",
    title: "Two Sum",
    difficulty: "EASY",
    topics: ["Array"],
    solveState: "solved",
    note: "Nhớ dùng HashMap thay vì hai vòng lặp lồng nhau.",
    savedAtLabel: "20/09/2026",
  },
  {
    id: "139",
    code: "#139",
    title: "Word Break",
    difficulty: "MEDIUM",
    topics: ["DP", "String"],
    solveState: "attempted",
    note: "Ôn lại DP trên chuỗi trước khi thi giữa kỳ.",
    savedAtLabel: "18/09/2026",
  },
  {
    id: "895",
    code: "#895",
    title: "Maximum Frequency Stack",
    difficulty: "HARD",
    topics: ["Stack"],
    solveState: "todo",
    note: "",
    savedAtLabel: "12/09/2026",
    withdrawn: true,
  },
];

export function fetchSavedProblems(): SavedProblem[] {
  return SAVED.map((problem) => ({ ...problem }));
}

const STARTER: Record<string, string> = {
  stdio:
    "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // TODO: read stdin, write stdout\n    }\n}\n",
  function:
    "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // TODO: implement\n        return new int[0];\n    }\n}\n",
};

// Mock language multipliers (admin language config): Java 2x time/2x memory, C++ 1x, Python 3x time.
function limits(timeLimitMs: number, memoryLimitMb: number) {
  return {
    java: { timeLimitMs: timeLimitMs * 2, memoryLimitMb: memoryLimitMb * 2 },
    cpp: { timeLimitMs, memoryLimitMb },
    python: { timeLimitMs: timeLimitMs * 3, memoryLimitMb },
  };
}

const PROBLEM_DETAILS: Record<string, ProblemDetail> = {
  "1": {
    id: "1",
    code: "#1",
    title: "Two Sum",
    difficulty: "EASY",
    topics: ["Array"],
    submissionModel: "both",
    statementMd:
      "Cho một mảng số nguyên `nums` và một số nguyên `target`, trả về chỉ số của hai số sao cho tổng của chúng bằng `target`.\n\nGiả định mỗi input có đúng một lời giải, và không được dùng cùng một phần tử hai lần.\n\n## Ràng buộc\n\n- `2 <= nums.length <= 10^4`\n- `-10^9 <= nums[i] <= 10^9`\n- Chỉ có đúng một đáp án hợp lệ",
    examples: [
      {
        input: "nums = [2,7,11,15], target = 9",
        output: "[0,1]",
        explanation: "nums[0] + nums[1] = 2 + 7 = 9",
      },
      { input: "nums = [3,2,4], target = 6", output: "[1,2]" },
      { input: "nums = [3,3], target = 6", output: "[0,1]" },
    ],
    limitsByLanguage: limits(1000, 256),
    acRate: 74,
    saved: true,
    hiddenTestcaseCount: 9,
    starterCode: {
      "java:stdio": STARTER.stdio,
      "java:function":
        "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[0];\n    }\n}\n",
      "cpp:stdio":
        "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // TODO: read stdin, write stdout\n    return 0;\n}\n",
      "cpp:function":
        "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {};\n    }\n};\n",
      "python:stdio":
        'def main():\n    # TODO: read stdin, write stdout\n    pass\n\nif __name__ == "__main__":\n    main()\n',
      "python:function":
        "class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        return []\n",
    } as Record<StarterCodeKey, string>,
    sampleTestcases: [
      { input: "[2,7,11,15]\n9", expectedOutput: "[0,1]" },
      { input: "[3,2,4]\n6", expectedOutput: "[1,2]" },
      { input: "[3,3]\n6", expectedOutput: "[0,1]" },
    ],
    mySubmissions: [
      {
        id: "s1",
        verdict: "accepted",
        passedCount: 12,
        totalCount: 12,
        language: "java",
        runtimeMs: 4,
        memoryKb: 38400,
        submittedAt: "2026-09-20T07:02:00Z",
      },
      {
        id: "s2",
        verdict: "wrongAnswer",
        passedCount: 2,
        totalCount: 12,
        language: "java",
        runtimeMs: null,
        memoryKb: null,
        submittedAt: "2026-09-20T06:58:00Z",
      },
    ],
    solutionReview: {
      timeComplexity: "O(n)",
      spaceComplexity: "O(n)",
      notes: [
        "Dùng HashMap để tra cứu phần bù trong một lượt duyệt.",
        "Không có lỗi biên đáng chú ý.",
      ],
    },
  },
  "121": {
    id: "121",
    code: "#121",
    title: "Best Time to Buy and Sell Stock",
    difficulty: "EASY",
    topics: ["Array"],
    submissionModel: "both",
    mockAlwaysPass: true,
    statementMd:
      "Cho mảng `prices` với `prices[i]` là giá cổ phiếu ở ngày thứ `i`. Chọn một ngày để mua và một ngày sau đó để bán sao cho lợi nhuận lớn nhất; trả về lợi nhuận, hoặc `0` nếu không có lãi.\n\n## Ràng buộc\n\n- `1 <= prices.length <= 10^5`\n- `0 <= prices[i] <= 10^4`",
    examples: [
      {
        input: "prices = [7,1,5,3,6,4]",
        output: "5",
        explanation: "Mua ngày 2 (giá 1), bán ngày 5 (giá 6): lãi 5.",
      },
      { input: "prices = [7,6,4,3,1]", output: "0" },
    ],
    limitsByLanguage: limits(1000, 256),
    acRate: 68,
    saved: false,
    hiddenTestcaseCount: 20,
    starterCode: {
      "java:stdio": STARTER.stdio,
      "java:function":
        "class Solution {\n    public int maxProfit(int[] prices) {\n        return 0;\n    }\n}\n",
      "cpp:stdio":
        "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
      "cpp:function":
        "class Solution {\npublic:\n    int maxProfit(vector<int>& prices) {\n        return 0;\n    }\n};\n",
      "python:stdio":
        'def main():\n    pass\n\nif __name__ == "__main__":\n    main()\n',
      "python:function":
        "class Solution:\n    def maxProfit(self, prices: list[int]) -> int:\n        return 0\n",
    } as Record<StarterCodeKey, string>,
    sampleTestcases: [
      { input: "[7,1,5,3,6,4]", expectedOutput: "5" },
      { input: "[7,6,4,3,1]", expectedOutput: "0" },
      { input: "[1,2]", expectedOutput: "1" },
      { input: "[2,1]", expectedOutput: "0" },
      { input: "[1]", expectedOutput: "0" },
      { input: "[2,4,1]", expectedOutput: "2" },
      { input: "[3,2,6,5,0,3]", expectedOutput: "4" },
      { input: "[1,2,3,4,5]", expectedOutput: "4" },
      { input: "[5,5,5,5]", expectedOutput: "0" },
      { input: "[2,1,2,1,0,1,2]", expectedOutput: "2" },
    ],
    mySubmissions: [],
    solutionReview: null,
  },
  "139": {
    id: "139",
    code: "#139",
    title: "Word Break",
    difficulty: "MEDIUM",
    topics: ["DP", "String"],
    submissionModel: "both",
    statementMd:
      "Cho một chuỗi `s` và một từ điển `wordDict`, trả về `true` nếu `s` có thể tách thành một dãy các từ trong từ điển.\n\n## Ràng buộc\n\n- `1 <= s.length <= 300`\n- `1 <= wordDict.length <= 1000`",
    examples: [
      { input: 's = "leetcode", wordDict = ["leet","code"]', output: "true" },
    ],
    limitsByLanguage: limits(2000, 256),
    acRate: 46,
    saved: true,
    hiddenTestcaseCount: 11,
    starterCode: {
      "java:stdio": STARTER.stdio,
      "java:function":
        "class Solution {\n    public boolean wordBreak(String s, List<String> wordDict) {\n        return false;\n    }\n}\n",
      "cpp:stdio":
        "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
      "cpp:function":
        "class Solution {\npublic:\n    bool wordBreak(string s, vector<string>& wordDict) {\n        return false;\n    }\n};\n",
      "python:stdio":
        'def main():\n    pass\n\nif __name__ == "__main__":\n    main()\n',
      "python:function":
        "class Solution:\n    def wordBreak(self, s: str, wordDict: list[str]) -> bool:\n        return False\n",
    } as Record<StarterCodeKey, string>,
    sampleTestcases: [
      { input: 'leetcode\n["leet","code"]', expectedOutput: "true" },
    ],
    mySubmissions: [
      {
        id: "s3",
        verdict: "wrongAnswer",
        passedCount: 6,
        totalCount: 12,
        language: "python",
        runtimeMs: null,
        memoryKb: null,
        submittedAt: "2026-09-19T02:11:00Z",
      },
    ],
    solutionReview: null,
  },
};

export function fetchProblemDetail(
  problemId: string,
): ProblemDetail | undefined {
  const found = PROBLEM_DETAILS[problemId];
  if (found) return structuredClone(found);
  // ponytail: only two problems have a hand-written statement. Every other catalogue row gets the
  // Two Sum scaffold (starter code, samples) under its own title, so the list never dead-ends on
  // "not found". Real statements come from the problem-bank API.
  const item = PROBLEMS.find((problem) => problem.id === problemId);
  if (!item) return undefined;
  return {
    ...structuredClone(PROBLEM_DETAILS["1"]!),
    id: item.id,
    code: item.code,
    title: item.title,
    difficulty: item.difficulty,
    topics: item.topics,
    submissionModel: item.submissionModel,
    acRate: item.acRate,
    saved: false,
    statementMd: `Đề bài của **${item.title}** chưa được nhập vào dữ liệu giả lập; khung bên dưới dùng bộ mẫu của Two Sum để chạy thử giao diện.

## Ràng buộc

- Dữ liệu giả lập, không phản ánh đề thật`,
    mySubmissions: [],
    solutionReview: null,
  };
}

/**
 * ponytail: no real judge, no STOMP channel. Odd attempts fail from the 5th testcase on and even attempts
 * are accepted, so both UI branches show without grading logic; the view reveals the finished script after
 * a fixed delay. Swap for the real channel at graduation (`shared/api/stomp` per BD Sheet 4.5).
 */
export function simulateSubmission(
  totalCount: number,
  attempt: number,
): SubmissionRunResult {
  const failAt = attempt % 2 === 1 ? Math.min(4, totalCount) : totalCount;
  const testcases = Array.from({ length: totalCount }, (_, index) => ({
    order: index + 1,
    verdict: (index < failAt ? "passed" : "failed") as "passed" | "failed",
  }));
  const passedCount = testcases.filter((tc) => tc.verdict === "passed").length;
  return {
    submissionId: `sub-${attempt}`,
    overallVerdict: passedCount === totalCount ? "accepted" : "wrongAnswer",
    passedCount,
    totalCount,
    testcases,
    runtimeMs: 12,
    memoryKb: 41984,
  };
}

/**
 * ponytail: no real judge for Run Code either. Odd runs fail the last sample with a wrong output so both
 * the comparison block and the pass state are visible; even runs pass everything. Only Sample testcases
 * ever appear here (BD Sheet 9 NO 3/4); swap for the synchronous `RunSampleTestcases` call at graduation.
 */
export function simulateSampleRun(
  samples: { input: string; expectedOutput: string }[],
  attempt: number,
): SampleRunResult {
  return {
    cases: samples.map((sample, index) => {
      const wrong = attempt % 2 === 1 && index === samples.length - 1;
      return {
        input: sample.input,
        expectedOutput: sample.expectedOutput,
        actualOutput: wrong ? "[]" : sample.expectedOutput,
        passed: !wrong,
        runtimeMs: 3 + index * 2,
        memoryKb: 36864 + index * 512,
      };
    }),
  };
}
