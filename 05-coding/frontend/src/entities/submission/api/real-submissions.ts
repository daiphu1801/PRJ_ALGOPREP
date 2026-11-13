import { api, isNotFound } from "@/shared/api";
import type {
  ListMySubmissionsParams,
  SubmissionDetail,
  SubmissionLanguage,
  SubmissionListPage,
  SubmissionMode,
  SubmissionStatus,
  TestcaseVerdict,
} from "../model/types";

type Problem = {
  problemId: string;
  code: string;
  title: string;
  slug: string;
  levelDisplayName: string;
  withdrawn: boolean;
};
export type WireDetail = {
  studentId?: string;
  studentName?: string;
  classId?: string;
  className?: string;
  leftClass?: boolean;
  submissionId: string;
  status: SubmissionStatus;
  language: SubmissionLanguage;
  submissionModel: SubmissionMode;
  submittedAt: string;
  sourceCode: string;
  compileErrorMessage: string | null;
  passedCount: number | null;
  totalCount: number | null;
  runtimeMs: number | null;
  memoryKb: number | null;
  beatsPercent: number | null;
  beatsSampleSize: number | null;
  problem: Problem | null;
  testcaseDetail: {
    available: boolean;
    unavailableReason: string | null;
    items: {
      order: number;
      testcaseId: string | null;
      visibility: "SAMPLE" | "HIDDEN";
      verdict: TestcaseVerdict;
      runtimeMs: number;
      memoryKb: number;
      actualOutput: string | null;
    }[];
  };
};
export async function getRealSubmission(
  id: string,
  area: "student" | "instructor",
): Promise<SubmissionDetail | null> {
  try {
    const path =
      area === "instructor"
        ? "/judge/instructor/submissions"
        : "/judge/submissions";
    const response = await api.get<WireDetail | { detail: WireDetail }>(
      `${path}/${encodeURIComponent(id)}`,
    );
    const detail = "detail" in response ? response.detail : response;
    const sampleContents = new Map<
      string,
      { input?: string; expectedOutput?: string }
    >();
    if (
      detail.problem &&
      !detail.problem.withdrawn &&
      detail.testcaseDetail.items.some((item) => item.visibility === "SAMPLE")
    ) {
      try {
        const samples = await api.get<{
          items: {
            testcaseId: string;
            input: string | null;
            expectedOutput: string | null;
          }[];
        }>(
          `/problem-bank/problems/${encodeURIComponent(detail.problem.problemId)}/sample-testcases`,
        );
        for (const sample of samples.items)
          sampleContents.set(sample.testcaseId, {
            input: sample.input ?? undefined,
            expectedOutput: sample.expectedOutput ?? undefined,
          });
      } catch {
        /* The persisted verdict remains readable when problem metadata is unavailable. */
      }
    }
    return {
      instructorContext:
        area === "instructor" && detail.studentId && detail.classId
          ? {
              studentId: detail.studentId,
              studentName: detail.studentName ?? "—",
              classId: detail.classId,
              className: detail.className ?? "—",
              leftClass: detail.leftClass ?? false,
            }
          : undefined,
      id: detail.submissionId,
      problemId: detail.problem?.problemId ?? "",
      problemSlug: detail.problem?.slug ?? "",
      problemTitle: detail.problem?.title ?? "—",
      difficulty: detail.problem?.levelDisplayName ?? "",
      problemWithdrawn: detail.problem?.withdrawn ?? true,
      language: detail.language,
      submissionMode: detail.submissionModel,
      status: detail.status,
      submittedAt: detail.submittedAt,
      sourceCode: detail.sourceCode,
      compileErrorMessage: detail.compileErrorMessage,
      passedCount: detail.passedCount,
      totalCount: detail.totalCount,
      runtimeMs: detail.runtimeMs,
      memoryKb: detail.memoryKb,
      beatsPercent: detail.beatsPercent,
      beatsSampleSize: detail.beatsSampleSize ?? undefined,
      testcaseDetailsAvailable: detail.testcaseDetail.available,
      testcaseUnavailableReason: detail.testcaseDetail.unavailableReason,
      testcases: detail.testcaseDetail.items.map((item) => ({
        testcaseId:
          item.visibility === "SAMPLE"
            ? (item.testcaseId ?? `sample-${item.order}`)
            : `hidden-${item.order}`,
        order: item.order,
        visibility: item.visibility,
        verdict: item.verdict,
        runtimeMs: item.runtimeMs,
        memoryKb: item.memoryKb,
        ...(item.visibility === "SAMPLE"
          ? {
              sampleActualOutput: item.actualOutput ?? undefined,
              sampleInput: sampleContents.get(item.testcaseId ?? "")?.input,
              sampleExpectedOutput: sampleContents.get(item.testcaseId ?? "")
                ?.expectedOutput,
            }
          : {}),
      })),
    };
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

export async function getRealMySubmissions(
  params: ListMySubmissionsParams,
): Promise<SubmissionListPage> {
  const response = await api.get<{
    page: number;
    pageSize: number;
    totalCount: number;
    items: {
      submissionId: string;
      problem: Problem | null;
      language: SubmissionLanguage;
      status: SubmissionStatus;
      passedCount: number | null;
      totalCount: number | null;
      runtimeMs: number | null;
      submittedAt: string;
    }[];
  }>("/judge/me/submissions", {
    q: params.query,
    verdict: params.verdict,
    language: params.language,
    page: params.page,
  });
  return {
    currentPage: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalCount,
    items: response.items.map((item) => ({
      id: item.submissionId,
      problemId: item.problem?.problemId ?? "",
      problemSlug: item.problem?.slug ?? "",
      problemCode: item.problem?.code ?? "—",
      problemTitle: item.problem?.title ?? "—",
      difficulty: item.problem?.levelDisplayName ?? "",
      problemWithdrawn: item.problem?.withdrawn ?? true,
      language: item.language,
      status: item.status,
      passedCount: item.passedCount,
      totalCount: item.totalCount,
      runtimeMs: item.runtimeMs,
      submittedAt: item.submittedAt,
    })),
  };
}
