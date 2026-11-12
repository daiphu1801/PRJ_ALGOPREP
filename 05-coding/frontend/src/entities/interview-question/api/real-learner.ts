// Real calls of the learner side of the bank (03-dd/api/F6_interview-bank.md #2 to #5, study mode).
// Any signed-in user may call them. Error codes: INB-010 (question gone) on #3 to #5, INB-V04 on #4,
// INB-V05 on #5; #2 has none. The wire is the controller record
// (`InterviewQuestionsController`): `page.number/size/totalItems/totalPages`, `meta.shown/total`.
import { ApiError, api } from "@/shared/api";
import type {
  DrillCard,
  LearnerQuestionDetail,
  LearnerQuestionFilters,
  LearnerQuestionItem,
  LearnerQuestionPage,
  QuestionRecall,
} from "../model/learner-types";
import {
  fromApiRating,
  toApiRating,
  toApiStatus,
  type ApiRating,
} from "../model/recall-map";
import type { RecallLevel } from "../model/types";
import { splitOutline } from "../lib/split-outline";

const BASE = "/interview-bank/questions";

/** Drill is a fixed 10 cards (USR0401 Q9, owner 2026-10-18). */
export const DRILL_SIZE = 10;

type RefDto = { code: string; displayName: string };
type ItemDto = {
  id: string;
  topic: RefDto;
  level: RefDto;
  title: string;
  recallRating: ApiRating | null;
  bookmarked: boolean;
};
type DrillDto = ItemDto & {
  suggestedApproach: string | null;
  coreKeywords: string[];
};
type RecallDto = {
  rating: ApiRating;
  intervalDays: number;
  nextReviewAt: string;
  ratedAt: string;
};
type DetailDto = {
  id: string;
  topic: RefDto & { usesStarFramework: boolean };
  level: RefDto;
  title: string;
  contentMarkdown: string | null;
  suggestedApproach: string | null;
  sampleAnswerFramework: string | null;
  coreKeywords: string[];
  followUpQuestions: string[];
  bookmarked: boolean;
  practiceAvailable: boolean;
  recall: RecallDto | null;
};

export function toItem(dto: ItemDto): LearnerQuestionItem {
  return {
    id: dto.id,
    topic: dto.topic.code,
    level: dto.level.code,
    title: dto.title,
    recall: fromApiRating(dto.recallRating),
    bookmarked: dto.bookmarked,
  };
}

export function toRecall(dto: RecallDto): QuestionRecall {
  return {
    level: fromApiRating(dto.rating) ?? "vague",
    intervalDays: dto.intervalDays,
    nextReviewAt: dto.nextReviewAt,
    ratedAt: dto.ratedAt,
  };
}

/** Empty or `all` filters are left out of the query string, so the server applies its defaults. */
function filterParams(filters: LearnerQuestionFilters) {
  const params: Record<string, string | number | boolean> = {};
  if (filters.q) params.q = filters.q;
  if (filters.status !== "all") params.status = toApiStatus(filters.status);
  if (filters.topic !== "all") params.topic = filters.topic;
  if (filters.level !== "all") params.level = filters.level;
  if (filters.bookmarkedOnly) params.bookmarkedOnly = true;
  return params;
}

/** #2: one page of 20 (the page size is fixed server-side). */
export async function realListQuestions(
  filters: LearnerQuestionFilters,
  page: number,
): Promise<LearnerQuestionPage> {
  const body = await api.get<{
    items: ItemDto[];
    page: { number: number; size: number; totalItems: number; totalPages: number };
    meta: { shown: number; total: number };
  }>(BASE, { ...filterParams(filters), page });
  return {
    items: body.items.map(toItem),
    number: body.page.number,
    totalPages: body.page.totalPages,
    shown: body.meta.shown,
    total: body.meta.total,
  };
}

/** #2 with `drill=true`: the server orders and cuts the cards; nothing is re-sorted here. */
export async function realListDrillCards(
  filters: LearnerQuestionFilters,
): Promise<DrillCard[]> {
  const body = await api.get<{ items: DrillDto[] }>(BASE, {
    ...filterParams(filters),
    drill: true,
    limit: DRILL_SIZE,
  });
  return body.items.map((dto) => ({
    ...toItem(dto),
    suggestedApproach: splitOutline(dto.suggestedApproach),
    coreKeywords: dto.coreKeywords,
  }));
}

/** #3: `null` when the question is gone (INB-010), every other failure is rethrown. */
export async function realGetQuestion(
  id: string,
): Promise<LearnerQuestionDetail | null> {
  try {
    const dto = await api.get<DetailDto>(`${BASE}/${id}`);
    return {
      id: dto.id,
      topic: dto.topic.code,
      usesStarFramework: dto.topic.usesStarFramework,
      level: dto.level.code,
      title: dto.title,
      content: dto.contentMarkdown ?? "",
      suggestedApproach: splitOutline(dto.suggestedApproach),
      sampleAnswerFramework: dto.sampleAnswerFramework ?? "",
      coreKeywords: dto.coreKeywords,
      followUps: dto.followUpQuestions,
      bookmarked: dto.bookmarked,
      practiceAvailable: dto.practiceAvailable,
      recall: dto.recall ? toRecall(dto.recall) : null,
    };
  } catch (error) {
    if (error instanceof ApiError && error.code === "INB-010") return null;
    throw error;
  }
}

/** #4: not idempotent (every call resets the review date), so callers never resend. */
export async function realRateRecall(
  id: string,
  level: RecallLevel,
): Promise<QuestionRecall> {
  return toRecall(
    await api.put<RecallDto>(`${BASE}/${id}/recall`, {
      rating: toApiRating(level),
    }),
  );
}

/** #5: sends the wanted state, not a toggle, so two quick clicks cannot undo each other. */
export async function realSetBookmark(
  id: string,
  bookmarked: boolean,
): Promise<boolean> {
  const body = await api.put<{ questionId: string; bookmarked: boolean }>(
    `${BASE}/${id}/bookmark`,
    { bookmarked },
  );
  return body.bookmarked;
}
