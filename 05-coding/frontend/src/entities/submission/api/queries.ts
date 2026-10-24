// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 41.
//
// Data boundary of the submission screens. Mock = the fixtures in __mock__/submission-mocks.ts;
// real = the F4 endpoints below, still `notImplemented` until the screens are wired to the backend.
import { useMockQuery } from "@/shared/api";
import {
  fetchMySubmissionsPage,
  fetchSubmissionDetail,
  isSubmissionInInstructorScope,
  READABLE_SUBMISSION_IDS,
} from "./__mock__/submission-mocks";
import type { ListMySubmissionsParams, SubmissionDetail } from "../model/types";

const JUDGE_SPEC = "03-dd/api/F4_judge-orchestration.md";

/** `ListMySubmissions` (F4 section 10, `GET /me/submissions`). Filters and paging are sent as query params. */
export const useMySubmissionsPage = (params: ListMySubmissionsParams) =>
  useMockQuery({
    queryKey: ["submission", "mine", params],
    mock: () => fetchMySubmissionsPage(params),
    spec: JUDGE_SPEC,
  });

/**
 * `GetSubmissionDetail` (F4 section 8 for A1, section 14 for A2). `null` = unknown id; for the
 * instructor area also an id outside the instructor's classes, which the real endpoint answers
 * with 404 (no existence leak), so the scope check lives in the mock only.
 */
export const useSubmissionDetail = (
  submissionId: string,
  area: "student" | "instructor" = "student",
) =>
  useMockQuery<SubmissionDetail | null>({
    queryKey: ["submission", "detail", area, submissionId],
    mock: () =>
      area === "instructor" && !isSubmissionInInstructorScope(submissionId)
        ? null
        : fetchSubmissionDetail(submissionId),
    spec: JUDGE_SPEC,
    enabled: Boolean(submissionId),
  });

/**
 * Submissions the instructor may read, one detail each: backs the instructor list
 * (`ListInstructorSubmissions`, F4 section 13) and the per-student history (section 15).
 */
export const useInstructorReadableSubmissions = () =>
  useMockQuery<SubmissionDetail[]>({
    queryKey: ["submission", "instructor-readable"],
    mock: () =>
      READABLE_SUBMISSION_IDS.flatMap((id) => {
        const detail = fetchSubmissionDetail(id);
        return detail ? [detail] : [];
      }),
    spec: JUDGE_SPEC,
  });
