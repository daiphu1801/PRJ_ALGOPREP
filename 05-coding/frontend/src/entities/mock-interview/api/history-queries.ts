// Data boundary of the session history (F5 section 31A for the list, section 9 for one session): `withMockData(mock, real)`.
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import type {
  InterviewHistoryPage,
  InterviewHistoryQuery,
} from "../model/history-types";
import { fakeGetSessionDetail, fakeListHistory } from "./__mock__/mock-history";
import { realListHistory } from "./real-history";
import { realGetSession, type SessionDetail } from "./real-interview";

export const useInterviewHistory = (query: InterviewHistoryQuery) =>
  useQuery<InterviewHistoryPage>({
    queryKey: ["mock-interview", "history", query],
    queryFn: () =>
      withMockData(
        () => fakeListHistory(query),
        () => realListHistory(query),
      ),
    placeholderData: keepPreviousData,
  });

/** One finished session with its conversation and scorecard (section 9). */
export const useInterviewSessionDetail = (sessionId: string) =>
  useQuery<SessionDetail>({
    queryKey: ["mock-interview", "session-detail", sessionId],
    queryFn: () =>
      withMockData(
        () => fakeGetSessionDetail(sessionId),
        () => realGetSession(sessionId),
      ),
  });
