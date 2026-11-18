// F6 #8 `GET /interview-bank/me/topic-progress` (03-dd/api/F6_interview-bank.md section 8): per-topic
// question review progress of the caller, for the dashboard (USR0601 Khu vuc F NO 10-14). No
// parameters, no error code of its own (only the common 401). Wire = InterviewTopicProgressController.Item.
"use client";

import { api, useMockQuery } from "@/shared/api";

export type InterviewTopicProgress = {
  topicId: string;
  topicName: string;
  questionsAttempted: number;
  totalQuestions: number;
  /** ISO instant, or null when the learner never practised this topic. */
  lastPracticedAt: string | null;
};

type Dto = {
  items: (InterviewTopicProgress & {
    topicCode: string;
    sortOrder: number;
    completionPercent: number;
  })[];
};

export async function realGetInterviewTopicProgress(): Promise<
  InterviewTopicProgress[]
> {
  const body = await api.get<Dto>("/interview-bank/me/topic-progress");
  return body.items.map((i) => ({
    topicId: i.topicId,
    topicName: i.topicName,
    questionsAttempted: i.questionsAttempted,
    totalQuestions: i.totalQuestions,
    lastPracticedAt: i.lastPracticedAt,
  }));
}

const MOCK: InterviewTopicProgress[] = [
  {
    topicId: "iq-topic-1",
    topicName: "Hệ điều hành",
    questionsAttempted: 3,
    totalQuestions: 8,
    lastPracticedAt: "2026-11-01T03:00:00Z",
  },
  {
    topicId: "iq-topic-2",
    topicName: "Cơ sở dữ liệu",
    questionsAttempted: 0,
    totalQuestions: 6,
    lastPracticedAt: null,
  },
];

export const useInterviewTopicProgress = () =>
  useMockQuery<InterviewTopicProgress[]>({
    queryKey: ["interview-question", "topic-progress"],
    mock: () => MOCK,
    real: realGetInterviewTopicProgress,
    spec: "03-dd/api/F6_interview-bank.md",
  });
