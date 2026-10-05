// PROTOTYPE mock — no backend endpoint exists yet.
// Content from 09-layoutBase/Admin - Soạn đề bài.dc.html:381-405 (`state`).
// Reached only through entities/problem/api/queries.ts (`withMockData`), so wiring the backend is
// a change there; the screens never import from here.
import { ApiError } from "@/shared/api";
import { aiGenerationSettings } from "../../model/generation-settings-store";
import { assertViewerCanSeeProblem } from "../../model/mock-ownership";
import type {
  GenerateTestcasesInput,
  GenerateTestcasesResult,
  ProblemDraft,
  ProblemDraftRecord,
  ProblemSpec,
  Testcase,
} from "../../model/draft-types";

const LATENCY_MS = 400;

function wait(ms = LATENCY_MS): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Contract is unwritten (03-dd/api/problem-bank.md does not exist yet): BD events EVT-7..EVT-31 map
// to SaveProblemContent, EVT-33 to UpdateProblemPublishStatus, EVT-16 to GenerateTestcasesWithAi and
// the read to GetProblemForAuthoring.
//
// The preview opens in a NEW browser tab (BD SHR0202 Q6) and must show the last saved data, but a
// new tab does not share this module's memory. The mock therefore keeps the last save in
// localStorage; the real call reads it back from the server.
const SAVED_KEY = (problemId: string) => `algoprep.problem-draft.saved.${problemId}`;
const GENERATIONS_KEY = (problemId: string | undefined) => `algoprep.problem-ai-generations.${problemId ?? "new"}`;

type StoredRecord = Omit<ProblemDraftRecord, "aiGeneration">;

function remember(draft: ProblemDraft, problemId: string | undefined) {
  if (!problemId) return;
  try {
    const value: StoredRecord = { draft, savedAt: new Date().toISOString() };
    window.localStorage.setItem(SAVED_KEY(problemId), JSON.stringify(value));
  } catch {
    // Storage blocked or full: the next load falls back to the sample problem.
  }
}

function recall(problemId: string | undefined): StoredRecord | null {
  if (!problemId) return null;
  try {
    const raw = window.localStorage.getItem(SAVED_KEY(problemId));
    if (!raw) return null;
    const record = JSON.parse(raw) as StoredRecord;
    // A copy saved before the spec tab (or with the old three-row spec) gets the sample spec.
    if (!record.draft.spec?.signature || hasLegacyType(record.draft.spec)) record.draft.spec = sampleSpec();
    return record;
  } catch {
    return null;
  }
}

// A copy saved while `of` was still a plain string (before nested types) cannot be read any more.
function hasLegacyType(spec: ProblemSpec): boolean {
  const legacy = (type: { of?: unknown } | undefined): boolean =>
    !!type && (typeof type.of === "string" || legacy(type.of as { of?: unknown } | undefined));
  const { returnType, parameters } = spec.signature;
  return legacy(returnType) || parameters.some((parameter) => legacy(parameter.type));
}

/** `GetProblemForAuthoring`. Every id shows the sample problem until it has been saved here. */
export async function loadProblemDraft(problemId?: string): Promise<ProblemDraftRecord> {
  if (problemId) assertViewerCanSeeProblem(problemId);
  await wait(150);
  const aiGeneration = { used: generationsUsed(problemId), limit: generationLimit() };
  return { ...(recall(problemId) ?? { draft: sampleProblemDraft(), savedAt: null }), aiGeneration };
}

/** Persists the whole draft as an unpublished save. Rejects on a failed request. */
export async function saveProblemDraft(draft: ProblemDraft, problemId?: string): Promise<void> {
  await wait();
  remember(draft, problemId);
}

/**
 * Saves the draft and moves it to PUBLISHED. The checklist gate lives on the server in the real
 * call (BD Sheet 9 #9); this mock trusts the screen, which only enables the button when it passes.
 */
export async function publishProblem(draft: ProblemDraft, problemId?: string): Promise<void> {
  await wait();
  remember(draft, problemId);
}

// F2-14 mock. Each click asks the AI for a NEW script, so the first click returns batch A and the
// second batch B (a real second script yields other cases; the fixed seed only makes ONE script's
// output reproducible, RD step 5). A third click is refused: the per-problem quota is 2.
//
// Flip the outcome from the address bar while trying the flow:
//   ?aiMock=error  the script exceeds the output-size cap, nothing is added, no attempt is counted
//   ?aiMock=warn   only some inputs survive, and the largest case times out on the reference solution
//   (absent)       four drafts per batch, one input dropped
// The cap is the ADMIN setting on the AI config screen (default 2), read at call time.
const generationLimit = () => aiGenerationSettings.get().maxPerProblem;
const RUN_MS = 2400;

function aiMockMode(): string | null {
  try {
    return new URLSearchParams(window.location.search).get("aiMock");
  } catch {
    return null;
  }
}

function generationsUsed(problemId: string | undefined): number {
  try {
    return Number(window.localStorage.getItem(GENERATIONS_KEY(problemId))) || 0;
  } catch {
    return 0;
  }
}

type Generated = Omit<Testcase, "id" | "origin" | "approved" | "visibility">;

const BATCHES: Generated[][] = [
  [
    { input: 's = "xyzabcxyz", t = "zxa"', expected: '"xyza"', category: "typical" },
    { input: 's = "AABBCC", t = "ABC"', expected: '"ABBC"', category: "sorted" },
    { input: 's = "ZZZZZZZZZZ", t = "Z"', expected: '"Z"', category: "extreme" },
    { input: "s 100k ký tự, t 50k ký tự", expected: '"…" (dài 100000)', category: "boundaryHigh" },
  ],
  [
    { input: 's = "bbaa", t = "aab"', expected: '"baa"', category: "duplicate" },
    { input: 's = "ABCDEFG", t = "G"', expected: '"G"', category: "boundaryLow" },
    { input: 's = "abcabcabc", t = "cba"', expected: '"abc"', category: "typical" },
    { input: 's 100k ký tự "a", t 99999 ký tự "a"', expected: '"…" (dài 99999)', category: "extreme" },
  ],
];

/** `GenerateTestcasesWithAi`: the reference solution is never an input (RD F2-14 step 2). */
export async function generateTestcasesWithAi(input: GenerateTestcasesInput): Promise<GenerateTestcasesResult> {
  const used = generationsUsed(input.problemId);
  if (used >= generationLimit()) {
    throw new ApiError("AI_GENERATION_LIMIT_REACHED", 429, "Per-problem generation quota used up");
  }
  const mode = aiMockMode();
  if (mode === "error") {
    await wait(RUN_MS / 2);
    throw new ApiError("SCRIPT_OUTPUT_LIMIT_EXCEEDED", 422, "Generator script exceeded the output size limit");
  }
  await wait(RUN_MS);
  const batch = BATCHES[used] ?? BATCHES[BATCHES.length - 1]!;
  const kept = mode === "warn" ? batch.slice(0, 2) : batch;
  try {
    window.localStorage.setItem(GENERATIONS_KEY(input.problemId), String(used + 1));
  } catch {
    // Storage blocked: the quota simply is not remembered.
  }
  return {
    testcases: kept.map((row, index) => ({
      ...row,
      id: `tc-ai-${Date.now()}-${index}`,
      visibility: "hidden",
      origin: "ai",
      approved: false,
    })),
    dropped:
      mode === "warn"
        ? [
            { reason: "invalidConstraint", count: 1 },
            { reason: "timeout", count: 1 },
          ]
        : [{ reason: used === 0 ? "invalidConstraint" : "referenceFailed", count: 1 }],
    largestCaseWarning: mode === "warn",
  };
}

function sampleProblemDraft(): ProblemDraft {
  return {
    title: "Minimum Window Substring",
    body: [
      "## Đề bài",
      "",
      "Cho hai chuỗi `s` và `t`, hãy trả về cửa sổ nhỏ nhất trong `s`",
      "chứa toàn bộ ký tự của `t`, tính cả số lần lặp.",
      "",
      "Nếu không tồn tại cửa sổ như vậy, trả về chuỗi rỗng.",
      "",
      "## Yêu cầu",
      "",
      "Kết quả được bảo đảm là duy nhất.",
      "Lời giải mong đợi có độ phức tạp $O(|s| + |t|)$.",
    ].join("\n"),
    constraints: [
      "1 <= s.length, t.length <= 10^5",
      "s và t chỉ gồm chữ cái tiếng Anh in hoa và in thường",
    ].join("\n"),
    topic: "String",
    difficulty: "HARD",
    status: "published",
    limits: {
      timeLimitMs: 2000,
      memoryLimitMb: 256,
      outputLimitKb: 64,
      stackLimitMb: 8,
    },
    solutionLanguage: "Python",
    solution: [
      "from collections import Counter",
      "",
      "def min_window(s: str, t: str) -> str:",
      '    if not t or not s:',
      '        return ""',
      "    need = Counter(t)",
      "    missing = len(t)",
      "    best = (0, 0)",
      "    left = 0",
      "    for right, ch in enumerate(s, 1):",
      "        if need[ch] > 0:",
      "            missing -= 1",
      "        need[ch] -= 1",
      "        if missing:",
      "            continue",
      "        while need[s[left]] < 0:",
      "            need[s[left]] += 1",
      "            left += 1",
      "        if not best[1] or right - left < best[1] - best[0]:",
      "            best = (left, right)",
      "    return s[best[0]:best[1]]",
    ].join("\n"),
    examples: [
      {
        id: "ex-1",
        input: 's = "ADOBECODEBANC", t = "ABC"',
        output: '"BANC"',
        explanation: 'Cửa sổ nhỏ nhất "BANC" chứa đủ A, B và C của t.',
      },
      {
        id: "ex-2",
        input: 's = "a", t = "aa"',
        output: '""',
        explanation: "Chuỗi s chỉ có một ký tự a, không đủ hai lần lặp mà t yêu cầu.",
      },
    ],
    // Two author-written Sample rows plus three hidden ones, all approved; then four rows the
    // generator script produced, still unapproved. That is the state F2-14 leaves behind: the
    // checklist reads 5 of 8, so the screen shows why approval is not a formality.
    testcases: [
      { id: "tc-1", input: 's = "ADOBECODEBANC", t = "ABC"', expected: '"BANC"', visibility: "public", origin: "manual", approved: true },
      { id: "tc-2", input: 's = "a", t = "a"', expected: '"a"', visibility: "public", origin: "manual", approved: true },
      { id: "tc-3", input: 's = "a", t = "aa"', expected: '""', visibility: "hidden", origin: "manual", approved: true },
      { id: "tc-4", input: 's = "ab", t = "b"', expected: '"b"', visibility: "hidden", origin: "manual", approved: true },
      { id: "tc-5", input: 's = "aa", t = "aa"', expected: '"aa"', visibility: "hidden", origin: "manual", approved: true },
      { id: "tc-6", input: 's = "", t = "a"', expected: '""', visibility: "hidden", origin: "ai", approved: false, category: "degenerate" },
      { id: "tc-7", input: 's = "aaaaaaaa", t = "aa"', expected: '"aa"', visibility: "hidden", origin: "ai", approved: false, category: "duplicate" },
      { id: "tc-8", input: "s 100k ký tự lặp chu kỳ, t 12 ký tự", expected: '"…" (dài 34)', visibility: "hidden", origin: "ai", approved: false, category: "boundaryHigh" },
      { id: "tc-9", input: 's = "abc", t = "abc"', expected: '"abc"', visibility: "hidden", origin: "ai", approved: false, category: "boundaryLow" },
    ],
    // F2-18: the reference solution passed every testcase that existed when it was last run.
    solutionCheck: { ran: true, passed: 5, total: 5 },
    spec: sampleSpec(),
    aiBrief:
      "Bài này dạy kỹ thuật cửa sổ trượt với bộ đếm. Nếu người học kẹt, hỏi ngược về cách theo dõi số ký tự còn thiếu thay vì đưa thẳng vòng lặp.",
    // The mockup's third flag belongs to the cut tiered-hint feature — see the model file.
    aiGuards: { noFullCode: true, socraticOnly: true },
  };
}

// Spec of the sample problem (Minimum Window Substring): two strings in, one string out. One shared
// signature; Java and C++ spell it minWindow, Python min_window, derived from the shared name.
function sampleSpec(): ProblemSpec {
  return {
    signature: {
      functionName: "min_window",
      returnType: { kind: "STRING" },
      parameters: [
        { id: "p1", name: "s", type: { kind: "STRING" } },
        { id: "p2", name: "t", type: { kind: "STRING" } },
      ],
      nameOverrides: {},
    },
    stdinFormat: "Dòng 1: chuỗi `s`.\nDòng 2: chuỗi `t`.",
    stdoutFormat: "Một dòng: cửa sổ nhỏ nhất của `s` chứa đủ ký tự của `t`, hoặc dòng rỗng nếu không có.",
    matchingStrategy: "EXACT",
    epsilon: "",
  };
}
