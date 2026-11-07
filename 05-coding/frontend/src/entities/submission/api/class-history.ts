import { useInfiniteQuery } from "@tanstack/react-query";
import { api, isMockMode } from "@/shared/api";
import type { SubmissionStatus, SubmissionLanguage } from "../model/types";
import { SUBMISSION_LANGUAGE_LABEL } from "../model/result-filter";
type HistoryPage = { page: number; pageSize: number; totalCount: number; items: { submissionId: string; problemTitle: string; language: SubmissionLanguage; status: SubmissionStatus; scoreRatio: number | null; submittedAt: string }[] };
export function useStudentClassSubmissionHistory(classId: string, studentId: string) {
  return useInfiniteQuery({
    queryKey: ["submission", "class-history", classId, studentId],
    initialPageParam: 1, enabled: !isMockMode(), retry: false,
    queryFn: async ({ pageParam }) => {
      const page = await api.get<HistoryPage>(`/judge/instructor/classes/${encodeURIComponent(classId)}/students/${encodeURIComponent(studentId)}/submissions`, { page: pageParam });
      return { ...page, items: page.items.map((item) => ({ id: item.submissionId, submissionId: item.submissionId, problemTitle: item.problemTitle, language: SUBMISSION_LANGUAGE_LABEL[item.language], status: item.status, ratio: item.scoreRatio == null ? "—" : `${Math.round(item.scoreRatio * 100)}%`, submittedAtLabel: new Date(item.submittedAt).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }) })) };
    },
    getNextPageParam: (page) => page.page * page.pageSize < page.totalCount ? page.page + 1 : undefined,
  });
}
