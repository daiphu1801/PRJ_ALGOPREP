// PROTOTYPE mock — no backend endpoint exists yet.
// Content from 09-layoutBase/Admin - Soạn đề bài.dc.html:381-405 (`state`).
import type { ProblemDraft } from "../../model/types";

export function fetchProblemDraft(): ProblemDraft {
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
      "Lời giải mong đợi có độ phức tạp O(|s| + |t|).",
    ].join("\n"),
    constraints: [
      "1 <= s.length, t.length <= 10^5",
      "s và t chỉ gồm chữ cái tiếng Anh in hoa và in thường",
    ].join("\n"),
    topic: "String",
    difficulty: "hard",
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
    testcases: [
      { id: "tc-1", input: 's = "ADOBECODEBANC", t = "ABC"', expected: '"BANC"', visibility: "public" },
      { id: "tc-2", input: 's = "a", t = "a"', expected: '"a"', visibility: "public" },
      { id: "tc-3", input: 's = "a", t = "aa"', expected: '""', visibility: "public" },
      { id: "tc-4", input: 's = "ab", t = "b"', expected: '"b"', visibility: "hidden" },
      { id: "tc-5", input: 's = "aa", t = "aa"', expected: '"aa"', visibility: "hidden" },
      { id: "tc-6", input: 's = "cabwefgewcwaefgcf", t = "cae"', expected: '"cwae"', visibility: "hidden" },
      { id: "tc-7", input: "s 100k ký tự, t 26 ký tự phân biệt", expected: '"…" (dài 5.184)', visibility: "hidden" },
      { id: "tc-8", input: "s 100k ký tự lặp chu kỳ, t 12 ký tự", expected: '"…" (dài 34)', visibility: "hidden" },
    ],
    aiBrief:
      "Bài này dạy kỹ thuật cửa sổ trượt với bộ đếm. Nếu người học kẹt, hỏi ngược về cách theo dõi số ký tự còn thiếu thay vì đưa thẳng vòng lặp.",
    // The mockup's third flag belongs to the cut tiered-hint feature — see the model file.
    aiGuards: { noFullCode: true, socraticOnly: true },
  };
}
