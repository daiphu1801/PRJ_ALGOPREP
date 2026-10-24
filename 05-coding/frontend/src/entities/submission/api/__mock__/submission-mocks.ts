// PROTOTYPE mock — no backend endpoint exists yet (03-dd/api/F4_judge-orchestration.md not written).
// Deterministic, no timers, no randomness — same input always returns the same output.
//
// Values follow 02-bd/screens/users/USR0201_submission_result.md and USR0202_my_submissions.md.
import type {
  Difficulty,
  ListMySubmissionsParams,
  SubmissionDetail,
  SubmissionLanguage,
  SubmissionListItem,
  SubmissionListPage,
  TestcaseVerdict,
} from "../../model/types";

type ProblemRef = {
  problemId: string;
  problemSlug: string;
  problemCode: string;
  problemTitle: string;
  difficulty: Difficulty;
};

const PROBLEMS: ProblemRef[] = [
  {
    problemId: "1",
    problemSlug: "1",
    problemCode: "#1",
    problemTitle: "Two Sum",
    difficulty: "EASY",
  },
  {
    problemId: "435",
    problemSlug: "435",
    problemCode: "#435",
    problemTitle: "Non-overlapping Intervals",
    difficulty: "MEDIUM",
  },
  {
    problemId: "297",
    problemSlug: "297",
    problemCode: "#297",
    problemTitle: "Serialize and Deserialize Binary Tree",
    difficulty: "HARD",
  },
  {
    problemId: "139",
    problemSlug: "139",
    problemCode: "#139",
    problemTitle: "Word Break",
    difficulty: "MEDIUM",
  },
];

function problemOf(index: number): ProblemRef {
  return PROBLEMS[index % PROBLEMS.length]!;
}

// -- Detail fixtures, one per submission id, covering every layout branch the BD calls out --------

const DETAILS: Record<string, SubmissionDetail> = {
  // Verdict banner + run stats + Beats, fully judged, ACCEPTED.
  "SB-90412": {
    id: "SB-90412",
    ...PROBLEMS[0]!,
    language: "PYTHON",
    submissionMode: "FUNCTION_WRAPPER",
    status: "ACCEPTED",
    passedCount: 24,
    totalCount: 24,
    runtimeMs: 42,
    memoryKb: 15360,
    beatsPercent: 87,
    submittedAt: "2026-09-24T09:12:00+07:00",
    sourceCode:
      "def two_sum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target - n], i]\n        seen[n] = i\n    return []\n",
    compileErrorMessage: null,
    testcases: Array.from({ length: 24 }, (_, i) => ({
      testcaseId: `tc-${i + 1}`,
      order: i + 1,
      visibility: i < 3 ? "SAMPLE" : "HIDDEN",
      verdict: "AC",
      runtimeMs: 30 + i,
      memoryKb: 15000 + i * 10,
      ...(i < 3
        ? {
            sampleInput: `nums = [2,7,11,15], target = ${9 + i}`,
            sampleExpectedOutput: `[0,1]`,
            sampleActualOutput: `[0,1]`,
          }
        : {}),
    })),
  },

  // Partial score — F4-13 ratio, WRONG_ANSWER on some hidden testcases.
  "SB-90408": {
    id: "SB-90408",
    ...PROBLEMS[1]!,
    language: "JAVA",
    submissionMode: "STANDARD_IO",
    status: "WRONG_ANSWER",
    passedCount: 17,
    totalCount: 20,
    runtimeMs: 118,
    memoryKb: 40960,
    beatsPercent: null,
    submittedAt: "2026-09-24T08:47:00+07:00",
    sourceCode:
      "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // ... greedy by end time, off-by-one on touching intervals\n    }\n}\n",
    compileErrorMessage: null,
    testcases: Array.from({ length: 20 }, (_, i) => ({
      testcaseId: `tc-${i + 1}`,
      order: i + 1,
      visibility: i < 2 ? "SAMPLE" : "HIDDEN",
      verdict: i >= 17 ? "WA" : "AC",
      runtimeMs: 90 + i,
      memoryKb: 40000 + i * 20,
      ...(i < 2
        ? {
            sampleInput: `root = [3,9,20,null,null,15,7]`,
            sampleExpectedOutput: `[[3],[20,9],[15,7]]`,
            sampleActualOutput:
              i === 0 ? `[[3],[20,9],[15,7]]` : `[[3],[20,9],[7,15]]`,
          }
        : {}),
    })),
  },

  // COMPILE_ERROR — the testcase table must be hidden entirely (BD Sheet 6, Khu vực D NO 1).
  "SB-90403": {
    id: "SB-90403",
    ...PROBLEMS[2]!,
    language: "CPP",
    submissionMode: "FUNCTION_WRAPPER",
    status: "COMPILE_ERROR",
    passedCount: null,
    totalCount: null,
    runtimeMs: null,
    memoryKb: null,
    beatsPercent: null,
    submittedAt: "2026-09-24T08:30:00+07:00",
    sourceCode:
      'class Codec {\npublic:\n    string serialize(TreeNode* root) {\n        return ""\n    }\n};\n',
    compileErrorMessage:
      "main.cpp:5:19: error: expected ';' after return statement\n        return \"\"\n                  ^\n                  ;",
    testcases: [],
  },

  // Still judging — realtime table fills in as testcases finish.
  "SB-90420": {
    id: "SB-90420",
    ...PROBLEMS[3]!,
    language: "PYTHON",
    submissionMode: "STANDARD_IO",
    status: "JUDGING",
    passedCount: 5,
    totalCount: 12,
    runtimeMs: 55,
    memoryKb: 16200,
    beatsPercent: null,
    submittedAt: "2026-09-25T10:02:00+07:00",
    sourceCode:
      "def word_break(s, words):\n    dp = [True] + [False] * len(s)\n    # ...\n",
    compileErrorMessage: null,
    testcases: Array.from({ length: 5 }, (_, i) => ({
      testcaseId: `tc-${i + 1}`,
      order: i + 1,
      visibility: i < 2 ? "SAMPLE" : "HIDDEN",
      verdict: "AC",
      runtimeMs: 40 + i,
      memoryKb: 16000 + i * 10,
    })),
  },
};

// The mock submissions an instructor may read; the
// student history and the submission list of the instructor area are built from them.
export const READABLE_SUBMISSION_IDS = [
  "SB-90412",
  "SB-90408",
  "SB-90403",
  "SB-90420",
];

/** A2 reads a submission only when it belongs to a student in one of their classes. */
export function isSubmissionInInstructorScope(submissionId: string): boolean {
  return READABLE_SUBMISSION_IDS.includes(submissionId);
}

export function fetchSubmissionDetail(id: string): SubmissionDetail | null {
  const fixture = DETAILS[id];
  if (fixture) return fixture;
  // ponytail: only four ids have a hand-written fixture. Any other row of the history list gets a
  // detail derived from its list item, so "Kết quả" never dead-ends on "not found". Source code and
  // sample cases are placeholders; the real data comes from `judge.submissions`.
  const item = LIST_ITEMS.find((row) => row.id === id);
  return item ? detailFromListItem(item) : null;
}

const FAIL_VERDICT: Partial<
  Record<SubmissionListItem["status"], TestcaseVerdict>
> = {
  WRONG_ANSWER: "WA",
  TIME_LIMIT_EXCEEDED: "TLE",
  RUNTIME_ERROR: "RE",
};

const PLACEHOLDER_CODE: Record<SubmissionLanguage, string> = {
  PYTHON:
    "def solve(data):\n    # placeholder source for the mock\n    return data\n",
  CPP: "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    // placeholder source for the mock\n    return 0;\n}\n",
  JAVA: "public class Main {\n    public static void main(String[] args) {\n        // placeholder source for the mock\n    }\n}\n",
};

function detailFromListItem(item: SubmissionListItem): SubmissionDetail {
  const compileError = item.status === "COMPILE_ERROR";
  const total = item.totalCount ?? 0;
  const passed = item.passedCount ?? 0;
  const failVerdict = FAIL_VERDICT[item.status] ?? "WA";
  const order = Number(item.id.replace(/\D/g, "")) || 0;
  return {
    id: item.id,
    problemId: item.problemId,
    problemSlug: item.problemSlug,
    problemTitle: item.problemTitle,
    difficulty: item.difficulty,
    language: item.language,
    submissionMode: "FUNCTION_WRAPPER",
    status: item.status,
    passedCount: item.passedCount,
    totalCount: item.totalCount,
    runtimeMs: item.runtimeMs,
    memoryKb: compileError ? null : 14000 + (order % 20) * 512,
    beatsPercent: item.status === "ACCEPTED" ? 40 + (order % 55) : null,
    submittedAt: item.submittedAt,
    sourceCode: PLACEHOLDER_CODE[item.language],
    compileErrorMessage: compileError
      ? "main: error: expected ';' before '}' token"
      : null,
    testcases: compileError
      ? []
      : Array.from({ length: total }, (_, i) => ({
          testcaseId: `tc-${i + 1}`,
          order: i + 1,
          visibility: i < 2 ? ("SAMPLE" as const) : ("HIDDEN" as const),
          verdict: i < passed ? ("AC" as const) : failVerdict,
          runtimeMs: (item.runtimeMs ?? 30) - 8 + (i % 9),
          memoryKb: 14000 + i * 16,
          ...(i < 2
            ? {
                sampleInput: "input (mock)",
                sampleExpectedOutput: "expected (mock)",
                sampleActualOutput:
                  i < passed ? "expected (mock)" : "actual (mock)",
              }
            : {}),
        })),
  };
}

// -- List + stats for USR0202 -----------------------------------------------------------------

const STATUSES: SubmissionListItem["status"][] = [
  "ACCEPTED",
  "WRONG_ANSWER",
  "ACCEPTED",
  "TIME_LIMIT_EXCEEDED",
  "COMPILE_ERROR",
  "ACCEPTED",
  "RUNTIME_ERROR",
];
const LANGUAGES: SubmissionLanguage[] = ["PYTHON", "PYTHON", "JAVA", "CPP"];

const LIST_ITEMS: SubmissionListItem[] = Array.from({ length: 24 }, (_, i) => {
  const problem = problemOf(i);
  const status = STATUSES[i % STATUSES.length]!;
  const isAccepted = status === "ACCEPTED";
  const total = 20 + (i % 5);
  return {
    // The first 4 rows link to a real DETAILS fixture (so "View result" opens a populated screen);
    // every other row gets a plain unique id — cycling through DETAILS keys here caused duplicate
    // ids (and duplicate React keys) once the list grew past DETAILS' own size.
    id: i < 4 ? Object.keys(DETAILS)[i]! : `SB-90${300 + i}`,
    problemId: problem.problemId,
    problemSlug: problem.problemSlug,
    problemCode: problem.problemCode,
    problemTitle: problem.problemTitle,
    difficulty: problem.difficulty,
    language: LANGUAGES[i % LANGUAGES.length]!,
    status,
    passedCount:
      status === "COMPILE_ERROR"
        ? null
        : isAccepted
          ? total
          : Math.max(0, total - 3 - (i % 4)),
    totalCount: status === "COMPILE_ERROR" ? null : total,
    runtimeMs: status === "COMPILE_ERROR" ? null : 30 + i * 4,
    submittedAt: new Date(
      Date.UTC(2026, 8, 24, 10, 0, 0) - i * 3600_000,
    ).toISOString(),
  };
});

export function fetchMySubmissionsPage(
  params: ListMySubmissionsParams = {},
): SubmissionListPage {
  const {
    query = "",
    verdict = "ALL",
    language = "ALL",
    page = 1,
    pageSize = 20,
  } = params;
  const needle = query.trim().toLowerCase();

  const filtered = LIST_ITEMS.filter(
    (item) =>
      (verdict === "ALL" || item.status === verdict) &&
      (language === "ALL" || item.language === language) &&
      (!needle ||
        item.problemCode.toLowerCase().includes(needle) ||
        item.problemTitle.toLowerCase().includes(needle)),
  );

  const start = (page - 1) * pageSize;
  return {
    items: filtered.slice(start, start + pageSize),
    currentPage: page,
    pageSize,
    totalItems: filtered.length,
  };
}
