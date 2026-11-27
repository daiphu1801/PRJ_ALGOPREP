// F5 section 31A `GET /ai/me/interview-sessions`: the caller's COMPLETED and ABANDONED sessions, searched, filtered,
// sorted and paged by the server. A filter left on "all" is not sent.
import { api, type PageMeta } from "@/shared/api";
import type {
  HistoryStatus,
  InterviewHistoryItem,
  InterviewHistoryPage,
  InterviewHistoryQuery,
} from "../model/history-types";
import type { InterviewEntryType, InterviewerLevel } from "../model/types";

type ItemDto = {
  sessionId: string;
  entryType: "SUBMISSION" | "QUESTION_BANK" | "TOPIC_SELECTION";
  status: "COMPLETED" | "ABANDONED";
  questionText: string | null;
  topics: string[];
  interviewerLevel: "INTERN" | "JUNIOR" | "MIDDLE" | "SENIOR" | null;
  turnCount: number;
  maxTurns: number | null;
  startedAt: string;
  endedAt: string;
  overallScore: number | null;
};

const ENTRY_FROM_DTO: Record<ItemDto["entryType"], InterviewEntryType> = {
  SUBMISSION: "submission",
  QUESTION_BANK: "bank",
  TOPIC_SELECTION: "custom",
};
const ENTRY_TO_DTO: Record<InterviewEntryType, ItemDto["entryType"]> = {
  submission: "SUBMISSION",
  bank: "QUESTION_BANK",
  custom: "TOPIC_SELECTION",
};

function toItem(dto: ItemDto): InterviewHistoryItem {
  return {
    sessionId: dto.sessionId,
    entryType: ENTRY_FROM_DTO[dto.entryType],
    status: dto.status.toLowerCase() as HistoryStatus,
    questionText: dto.questionText,
    topics: dto.topics,
    level: dto.interviewerLevel
      ? (dto.interviewerLevel.toLowerCase() as InterviewerLevel)
      : null,
    turnCount: dto.turnCount,
    maxTurns: dto.maxTurns,
    startedAt: dto.startedAt,
    endedAt: dto.endedAt,
    overallScore: dto.overallScore,
  };
}

export async function realListHistory(
  query: InterviewHistoryQuery,
): Promise<InterviewHistoryPage> {
  const params: Record<string, string | number> = {
    sort: query.sort,
    order: query.order,
    page: query.page,
    size: query.size,
  };
  if (query.q.trim()) params.q = query.q.trim();
  if (query.entryType !== "all")
    params.entryType = ENTRY_TO_DTO[query.entryType];
  if (query.status !== "all") params.status = query.status.toUpperCase();
  if (query.level !== "all") params.level = query.level.toUpperCase();
  if (query.scoreBand !== "all") params.scoreBand = query.scoreBand;
  const body = await api.get<{ items: ItemDto[]; page: PageMeta }>(
    "/ai/me/interview-sessions",
    params,
  );
  return { items: body.items.map(toItem), page: body.page };
}
