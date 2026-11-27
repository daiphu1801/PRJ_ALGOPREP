// Canned session history for mock mode: a dozen sessions of every source, level and score band, filtered, sorted and
// paged here the way the server does it for section 31A.
import type {
  InterviewHistoryItem,
  InterviewHistoryPage,
  InterviewHistoryQuery,
} from "../../model/history-types";
import type { SessionDetail } from "../real-interview";
import { computeInterviewResult } from "./mock-interview-mocks";

const ROWS: InterviewHistoryItem[] = Array.from({ length: 12 }, (_, i) => {
  const entryType =
    (["bank", "custom", "submission"] as const)[i % 3] ?? "bank";
  const abandoned = i % 6 === 5;
  return {
    sessionId: `hist-${i + 1}`,
    entryType,
    status: abandoned ? "abandoned" : "completed",
    questionText: entryType === "bank" ? `Câu hỏi ngân hàng số ${i + 1}` : null,
    topics: entryType === "custom" ? ["Cơ sở dữ liệu", "Hệ điều hành"] : [],
    level:
      entryType === "submission"
        ? null
        : ((["junior", "middle", "senior", "intern"] as const)[i % 4] ??
          "junior"),
    turnCount: 3 + (i % 5),
    maxTurns: entryType === "submission" ? null : 8,
    startedAt: `2026-11-${String(22 - i).padStart(2, "0")}T09:00:00Z`,
    endedAt: `2026-11-${String(22 - i).padStart(2, "0")}T09:30:00Z`,
    overallScore: abandoned ? null : Math.round((2 + ((i * 7) % 8)) * 10) / 10,
  };
});

const band = (score: number | null) =>
  score === null ? null : score >= 8 ? "good" : score >= 5 ? "fair" : "weak";

export function fakeListHistory(
  query: InterviewHistoryQuery,
): Promise<InterviewHistoryPage> {
  const q = query.q.trim().toLowerCase();
  const rows = ROWS.filter(
    (r) =>
      (!q ||
        (r.questionText ?? "").toLowerCase().includes(q) ||
        r.topics.join(" ").toLowerCase().includes(q)) &&
      (query.entryType === "all" || r.entryType === query.entryType) &&
      (query.status === "all" || r.status === query.status) &&
      (query.level === "all" || r.level === query.level) &&
      (query.scoreBand === "all" || band(r.overallScore) === query.scoreBand),
  ).sort((a, b) => {
    const dir = query.order === "asc" ? 1 : -1;
    if (query.sort === "score") {
      if (a.overallScore === null) return 1;
      if (b.overallScore === null) return -1;
      return (a.overallScore - b.overallScore) * dir;
    }
    return a.startedAt.localeCompare(b.startedAt) * dir;
  });
  const start = (query.page - 1) * query.size;
  return Promise.resolve({
    items: rows.slice(start, start + query.size),
    page: {
      number: query.page,
      size: query.size,
      totalItems: rows.length,
      totalPages: Math.max(1, Math.ceil(rows.length / query.size)),
    },
  });
}

export function fakeGetSessionDetail(
  sessionId: string,
): Promise<SessionDetail> {
  const row = ROWS.find((r) => r.sessionId === sessionId);
  if (!row) return Promise.reject(new Error("session not found"));
  const turns: SessionDetail["turns"] = [
    {
      turnIndex: 1,
      role: "ai",
      stage: "explain",
      content: "Hãy giải thích cách bạn tiếp cận bài này.",
    },
    {
      turnIndex: 2,
      role: "user",
      stage: "explain",
      content: "Mình dùng bảng băm để tra cứu trong O(1).",
    },
    {
      turnIndex: 3,
      role: "ai",
      stage: "challenge",
      content: "Nếu có nhiều khóa trùng nhau thì sao?",
    },
    {
      turnIndex: 4,
      role: "user",
      stage: "challenge",
      content: "Mình xử lý bằng danh sách liên kết ở mỗi ô.",
    },
  ];
  return Promise.resolve({
    session: {
      sessionId,
      stage: row.status,
      turnCount: row.turnCount,
      maxTurns: row.maxTurns,
      hintAllowed: true,
      level: row.level,
      startedAt: row.startedAt,
    },
    replyState: "IDLE",
    turns,
    result: row.status === "completed" ? computeInterviewResult() : null,
  });
}
