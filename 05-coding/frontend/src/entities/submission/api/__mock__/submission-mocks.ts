// PROTOTYPE mock — no backend endpoint exists yet (03-dd/api/judge-orchestration.md not written).
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
  SubmissionStats,
} from "../../model/types";

type ProblemRef = {
  problemId: string;
  problemSlug: string;
  problemCode: string;
  problemTitle: string;
  difficulty: Difficulty;
};

const PROBLEMS: ProblemRef[] = [
  { problemId: "p1", problemSlug: "two-sum", problemCode: "AP001", problemTitle: "Two Sum", difficulty: "EASY" },
  { problemId: "p2", problemSlug: "binary-tree-zigzag", problemCode: "AP014", problemTitle: "Duyệt zigzag cây nhị phân", difficulty: "MEDIUM" },
  { problemId: "p3", problemSlug: "lru-cache", problemCode: "AP027", problemTitle: "Thiết kế LRU Cache", difficulty: "HARD" },
  { problemId: "p4", problemSlug: "merge-intervals", problemCode: "AP009", problemTitle: "Gộp khoảng giao nhau", difficulty: "MEDIUM" },
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
      "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        // ... zigzag traversal, off-by-one on the last level\n    }\n}\n",
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
            sampleActualOutput: i === 0 ? `[[3],[20,9],[15,7]]` : `[[3],[20,9],[7,15]]`,
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
      "class LRUCache {\npublic:\n    LRUCache(int capacity) {\n        cap = capacity\n    }\n};\n",
    compileErrorMessage: "main.cpp:4:24: error: expected ';' after expression\n        cap = capacity\n                      ^\n                      ;",
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
    sourceCode: "def merge(intervals):\n    intervals.sort()\n    # ...\n",
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

export function fetchSubmissionDetail(id: string): SubmissionDetail | null {
  return DETAILS[id] ?? null;
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
    passedCount: status === "COMPILE_ERROR" ? null : isAccepted ? total : Math.max(0, total - 3 - (i % 4)),
    totalCount: status === "COMPILE_ERROR" ? null : total,
    runtimeMs: status === "COMPILE_ERROR" ? null : 30 + i * 4,
    submittedAt: new Date(Date.UTC(2026, 8, 24, 10, 0, 0) - i * 3600_000).toISOString(),
  };
});

export function fetchMySubmissionsPage(params: ListMySubmissionsParams = {}): SubmissionListPage {
  const { query = "", verdict = "ALL", language = "ALL", page = 1, pageSize = 20 } = params;
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

/**
 * Raw counters, computed once from LIST_ITEMS — mirrors the read model
 * `identity.user_submission_stats` (no `acceptedRate` column, per BD V0.2 fix).
 */
export function fetchMySubmissionStats(): SubmissionStats {
  const totalSubmissions = LIST_ITEMS.length;
  const acceptedCount = LIST_ITEMS.filter((item) => item.status === "ACCEPTED").length;
  // First-try acceptance is [SoT: Suy luận] here: the mock treats every 3rd accepted item as first-try.
  const firstTryAcceptedCount = LIST_ITEMS.filter(
    (item, i) => item.status === "ACCEPTED" && i % 3 === 0,
  ).length;

  const counts = new Map<SubmissionLanguage, number>();
  for (const item of LIST_ITEMS) counts.set(item.language, (counts.get(item.language) ?? 0) + 1);
  let topLanguage: SubmissionLanguage | null = null;
  let topLanguageCount = 0;
  for (const [lang, count] of counts) {
    if (count > topLanguageCount) {
      topLanguage = lang;
      topLanguageCount = count;
    }
  }

  return { totalSubmissions, acceptedCount, firstTryAcceptedCount, topLanguage, topLanguageCount };
}
