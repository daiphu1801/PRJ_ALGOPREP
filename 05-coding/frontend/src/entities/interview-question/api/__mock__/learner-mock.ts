// PROTOTYPE mock — stands in for the learner endpoints (03-dd/api/F6_interview-bank.md #2 to #7) while
// `NEXT_PUBLIC_MOCK_DATA=true`. It behaves like the server: it filters, pages and orders the in-memory
// bank, and it keeps the per-user facts (recall, bookmark, practice attempts) in module state, so a
// rating made on the detail screen is still there on the list. Resets on reload.
// The mock id of a question is its `IQ-nnn` code.
import { ApiError } from "@/shared/api";
import type {
  DrillCard,
  LearnerQuestionDetail,
  LearnerQuestionFilters,
  LearnerQuestionItem,
  LearnerQuestionPage,
  PracticeAttempt,
  PracticeAttemptPage,
  QuestionRecall,
} from "../../model/learner-types";
import {
  RECALL_NEXT_REVIEW_DAYS,
  type InterviewQuestion,
  type RecallLevel,
} from "../../model/types";
import { fetchInterviewQuestionPage, mockCriterionResults } from "./interview-question-mocks";

const MOCK_LATENCY_MS = 150;
const wait = () => new Promise((resolve) => setTimeout(resolve, MOCK_LATENCY_MS));

const PAGE_SIZE = 20;
const DAY_MS = 86_400_000;

type State = {
  recall: Record<string, QuestionRecall | null>;
  bookmarks: Record<string, boolean>;
  attempts: Record<string, PracticeAttempt[]>;
};
let state: State = { recall: {}, bookmarks: {}, attempts: {} };

/** Test helper: drop every user change and go back to the seeded values. */
export function resetRecallAndBookmarkState() {
  state = { recall: {}, bookmarks: {}, attempts: {} };
}

const bank = () => fetchInterviewQuestionPage().questions;

function recallOf(question: InterviewQuestion): QuestionRecall | null {
  if (question.code in state.recall) return state.recall[question.code] ?? null;
  return question.defaultRecall
    ? makeRecall(question.defaultRecall, Date.now())
    : null;
}

function makeRecall(level: RecallLevel, now: number): QuestionRecall {
  const intervalDays = RECALL_NEXT_REVIEW_DAYS[level];
  return {
    level,
    intervalDays,
    ratedAt: new Date(now).toISOString(),
    nextReviewAt: new Date(now + intervalDays * DAY_MS).toISOString(),
  };
}

function toItem(question: InterviewQuestion): LearnerQuestionItem {
  return {
    id: question.code,
    topic: question.topic,
    level: question.level,
    title: question.question,
    recall: recallOf(question)?.level ?? null,
    bookmarked: state.bookmarks[question.code] ?? false,
  };
}

function matches(question: InterviewQuestion, f: LearnerQuestionFilters) {
  const item = toItem(question);
  if (f.topic !== "all" && item.topic !== f.topic) return false;
  if (f.level !== "all" && item.level !== f.level) return false;
  if (f.bookmarkedOnly && !item.bookmarked) return false;
  if (f.status === "new" && item.recall !== null) return false;
  if (f.status === "known" && item.recall !== "known") return false;
  if (f.status === "reviewing" && item.recall !== "vague" && item.recall !== "forgotten")
    return false;
  const needle = f.q.trim().toLowerCase();
  return (
    !needle ||
    [question.question, question.content, ...question.coreKeywords].some((text) =>
      text.toLowerCase().includes(needle),
    )
  );
}

export async function mockListQuestions(
  filters: LearnerQuestionFilters,
  page: number,
): Promise<LearnerQuestionPage> {
  await wait();
  const all = bank();
  const hits = all.filter((question) => matches(question, filters));
  const totalPages = Math.max(1, Math.ceil(hits.length / PAGE_SIZE));
  const number = Math.min(Math.max(1, page), totalPages);
  return {
    items: hits.slice((number - 1) * PAGE_SIZE, number * PAGE_SIZE).map(toItem),
    number,
    totalPages,
    shown: hits.length,
    total: all.length,
  };
}

// Same order as the server (L6): due for review first, then never rated, then the rest.
const drillRank = (recall: RecallLevel | null) =>
  recall === null ? 1 : recall === "known" ? 2 : 0;

export async function mockListDrillCards(
  filters: LearnerQuestionFilters,
  limit: number,
): Promise<DrillCard[]> {
  await wait();
  return bank()
    .filter((question) => matches(question, filters))
    .map((question) => ({
      ...toItem(question),
      suggestedApproach: question.suggestedApproach,
      coreKeywords: question.coreKeywords,
    }))
    .sort((a, b) => drillRank(a.recall) - drillRank(b.recall))
    .slice(0, limit);
}

export async function mockGetQuestion(
  id: string,
): Promise<LearnerQuestionDetail | null> {
  await wait();
  const question = bank().find((candidate) => candidate.code === id);
  if (!question) return null;
  return {
    id: question.code,
    topic: question.topic,
    usesStarFramework: question.topic === "behavioural",
    level: question.level,
    title: question.question,
    content: question.content,
    suggestedApproach: question.suggestedApproach,
    sampleAnswerFramework: question.sampleAnswerFramework,
    coreKeywords: question.coreKeywords,
    followUps: question.followUps,
    bookmarked: state.bookmarks[question.code] ?? false,
    practiceAvailable: question.hasRubric,
    recall: recallOf(question),
  };
}

export async function mockRateRecall(
  id: string,
  level: RecallLevel,
): Promise<QuestionRecall> {
  await wait();
  const recall = makeRecall(level, Date.now());
  state = { ...state, recall: { ...state.recall, [id]: recall } };
  return recall;
}

export async function mockSetBookmark(
  id: string,
  bookmarked: boolean,
): Promise<boolean> {
  await wait();
  state = { ...state, bookmarks: { ...state.bookmarks, [id]: bookmarked } };
  return bookmarked;
}

function attemptsOf(question: InterviewQuestion): PracticeAttempt[] {
  return state.attempts[question.code] ?? question.attempts;
}

/** Practice feedback sample; the real text comes from the AI (`PRACTICE_FEEDBACK`). */
export async function mockSubmitPracticeAnswer(
  id: string,
  answerText: string,
): Promise<PracticeAttempt> {
  await wait();
  const question = bank().find((candidate) => candidate.code === id);
  if (!question) throw new ApiError("INB-010", 404);
  if (!question.hasRubric) throw new ApiError("INB-040", 422);
  const history = attemptsOf(question);
  const attempt: PracticeAttempt = {
    attemptNo: Math.max(0, ...history.map((a) => a.attemptNo)) + 1,
    createdAt: new Date().toISOString(),
    feedbackStatus: "completed",
    answerText,
    strengths: question.rubric
      .slice(0, 1)
      .map((c) => `Đã đề cập tốt: ${c.label}`),
    gaps: question.rubric
      .slice(1, 2)
      .map((c) => `Cần bổ sung thêm cho tiêu chí: ${c.label}`),
    nextSteps:
      "Xem lại Chế độ học để đối chiếu ý cần nói và khung trả lời chuẩn trước khi thử lại.",
    criteria: mockCriterionResults(question.rubric),
    feedbackErrorCode: null,
  };
  state = {
    ...state,
    attempts: { ...state.attempts, [id]: [attempt, ...history] },
  };
  return attempt;
}

const ATTEMPTS_PAGE = 10;

export async function mockListAttempts(
  id: string,
  before?: number,
): Promise<PracticeAttemptPage> {
  await wait();
  const question = bank().find((candidate) => candidate.code === id);
  if (!question) throw new ApiError("INB-010", 404);
  const all = [...attemptsOf(question)].sort((a, b) => b.attemptNo - a.attemptNo);
  const older = before === undefined ? all : all.filter((a) => a.attemptNo < before);
  return {
    items: older.slice(0, ATTEMPTS_PAGE),
    hasMore: older.length > ATTEMPTS_PAGE,
    totalAttempts: all.length,
  };
}
