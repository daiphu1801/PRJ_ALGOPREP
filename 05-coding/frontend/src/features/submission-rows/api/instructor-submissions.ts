import { useQuery } from "@tanstack/react-query";
import { api, isMockMode } from "@/shared/api";
import {
  SUBMISSION_LANGUAGE_LABEL,
  type SubmissionLanguage,
  type SubmissionStatus,
} from "@/entities/submission";
import type { SubmissionRow } from "../model/rows";

export function useInstructorSubmissionPage(params: {
  classId?: string;
  result: string;
  q: string;
  page: number;
}) {
  return useQuery({
    queryKey: ["submission", "instructor-page", params],
    enabled: !isMockMode(),
    retry: false,
    staleTime: 0,
    queryFn: async () => {
      const response = await api.get<{
        page: number;
        pageSize: number;
        totalCount: number;
        items: {
          submissionId: string;
          studentId: string;
          studentName: string;
          classId: string;
          className: string;
          problemTitle: string;
          language: SubmissionLanguage;
          status: SubmissionStatus;
          passedCount: number | null;
          totalCount: number | null;
          submittedAt: string;
          leftClass: boolean;
        }[];
      }>("/judge/instructor/submissions", params);
      return {
        ...response,
        items: response.items.map((item): SubmissionRow => ({
          key: `${item.classId}:${item.studentId}:${item.submissionId}`,
          submissionId: item.submissionId,
          studentId: item.studentId,
          studentName: item.studentName,
          classId: item.classId,
          className: item.className,
          leftClass: item.leftClass,
          problemTitle: item.problemTitle,
          language: SUBMISSION_LANGUAGE_LABEL[item.language],
          status: item.status,
          ratio:
            item.totalCount == null
              ? "-"
              : `${item.passedCount ?? 0}/${item.totalCount}`,
          submittedAt: Date.parse(item.submittedAt),
        })),
      };
    },
  });
}
