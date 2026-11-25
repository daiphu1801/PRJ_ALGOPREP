import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api, isMockMode } from "@/shared/api";
import type { SubmissionStatus, SubmissionLanguage } from "../model/types";
import { SUBMISSION_LANGUAGE_LABEL } from "../model/result-filter";
type HistoryPage = {
  page: number;
  pageSize: number;
  totalCount: number;
  items: {
    submissionId: string;
    problemTitle: string;
    language: SubmissionLanguage;
    status: SubmissionStatus;
    scoreRatio: number | null;
    submittedAt: string;
  }[];
};
/** One server page (20 rows) of the student's history in the class; the previous page stays while the next loads. */
export function useStudentClassSubmissionHistory(
  classId: string,
  studentId: string,
  pageNumber: number,
) {
  return useQuery({
    queryKey: ["submission", "class-history", classId, studentId, pageNumber],
    enabled: !isMockMode(),
    retry: false,
    placeholderData: keepPreviousData,
    queryFn: async () => {
      const page = await api.get<HistoryPage>(
        `/judge/instructor/classes/${encodeURIComponent(classId)}/students/${encodeURIComponent(studentId)}/submissions`,
        { page: pageNumber },
      );
      return {
        ...page,
        items: page.items.map((item) => ({
          id: item.submissionId,
          submissionId: item.submissionId,
          problemTitle: item.problemTitle,
          language: SUBMISSION_LANGUAGE_LABEL[item.language],
          status: item.status,
          ratio:
            item.scoreRatio == null
              ? "—"
              : `${Math.round(item.scoreRatio * 100)}%`,
          submittedAtLabel: new Date(item.submittedAt).toLocaleString("vi-VN", {
            dateStyle: "short",
            timeStyle: "short",
            timeZone: "Asia/Ho_Chi_Minh",
          }),
        })),
      };
    },
  });
}
