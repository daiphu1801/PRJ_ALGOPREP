import { useQuery } from "@tanstack/react-query";
import {
  api,
  ApiError,
  isMockMode,
  isNotFound,
  withMockData,
} from "@/shared/api";
import { fetchProblemDetail } from "./__mock__/problem-mocks";
import { overallVerdict } from "./real-execution";
import type {
  ProblemDetail,
  StarterCodeKey,
  SubmissionLanguage,
  WrapperMode,
} from "../model/types";

const languageCodes = { java: "JAVA", cpp: "CPP", python: "PYTHON" } as const;
const modelCodes = {
  function: "FUNCTION_WRAPPER",
  stdio: "STANDARD_IO",
} as const;
type WireLanguage = (typeof languageCodes)[SubmissionLanguage];

export async function fetchEnabledLanguages(): Promise<SubmissionLanguage[]> {
  const response = await api.get<{ items: WireLanguage[] }>(
    "/judge/languages/enabled",
  );
  const names = { JAVA: "java", CPP: "cpp", PYTHON: "python" } as const;
  if (
    !response.items.length ||
    response.items.some((item) => !Object.hasOwn(names, item))
  )
    throw new ApiError("UNKNOWN_ERROR", 503, "Invalid language response");
  return response.items.map((item) => names[item]);
}

export function useEnabledLanguages() {
  return useQuery({
    queryKey: ["judge", "enabled-languages"],
    initialData: isMockMode()
      ? (["java", "cpp", "python"] as SubmissionLanguage[])
      : undefined,
    queryFn: () =>
      withMockData(
        () => ["java", "cpp", "python"] as SubmissionLanguage[],
        fetchEnabledLanguages,
      ),
    staleTime: 0,
    retry: false,
  });
}

export async function fetchStarterCode(
  problemId: string,
  language: SubmissionLanguage,
  mode: WrapperMode,
) {
  const result = await api.get<{
    language: WireLanguage;
    submissionModel: string;
    sourceCode: string;
  }>(`/harness/problems/${encodeURIComponent(problemId)}/starter-code`, {
    language: languageCodes[language],
    model: modelCodes[mode],
  });
  if (
    result.language !== languageCodes[language] ||
    result.submissionModel !== modelCodes[mode] ||
    typeof result.sourceCode !== "string"
  )
    throw new ApiError("UNKNOWN_ERROR", 503, "Invalid starter response");
  return result.sourceCode;
}

export function useStarterCode(
  problemId: string,
  language: SubmissionLanguage,
  mode: WrapperMode,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["harness", "starter", problemId, language, mode],
    initialData: isMockMode()
      ? fetchProblemDetail(problemId)?.starterCode[`${language}:${mode}`]
      : undefined,
    queryFn: () =>
      withMockData(
        () =>
          fetchProblemDetail(problemId)?.starterCode[`${language}:${mode}`] ??
          "",
        () => fetchStarterCode(problemId, language, mode),
      ),
    enabled,
    staleTime: 0,
    retry: false,
  });
}

type WorkspaceResponse = {
  id: string;
  code: string;
  title: string;
  levelCode: string;
  topicNames: string[];
  statementMd: string;
  constraintsMd: string;
  functionWrapperSupported: boolean;
  acRate: number | null;
  bookmarked: boolean;
  workedExamples: {
    input: string;
    output: string;
    explanationMd: string | null;
  }[];
};

async function sampleText(
  inline: string | null,
  url: string | null,
): Promise<string> {
  if (inline !== null) return inline;
  if (!url) throw new ApiError("UNKNOWN_ERROR", 503, "Missing sample content");
  // Presigned object URL: do not attach the application bearer token.
  const response = await fetch(url, { credentials: "omit" });
  if (!response.ok)
    throw new ApiError("UNKNOWN_ERROR", 503, "Sample download failed");
  return response.text();
}

type LimitsResponse = {
  limits: {
    language: WireLanguage;
    timeLimitMs: number;
    memoryLimitMb: number;
  }[];
};
type HistoryResponse = {
  items: {
    submissionId: string;
    status: string;
    language: string;
    passedCount: number | null;
    totalCount: number | null;
    runtimeMs: number | null;
    memoryKb: number | null;
    submittedAt: string;
  }[];
};

const execPath = (problemId: string) =>
  `/judge/problems/${encodeURIComponent(problemId)}`;
const getLimits = (problemId: string) =>
  api.get<LimitsResponse>(`${execPath(problemId)}/execution-limits`);
const getHistory = (problemId: string) =>
  api.get<HistoryResponse>(`${execPath(problemId)}/submissions/mine`, {
    limit: 10,
  });

function toLimits(limits: LimitsResponse): ProblemDetail["limitsByLanguage"] {
  const byLanguage = Object.fromEntries(
    limits.limits.map((item) => [
      item.language.toLowerCase(),
      { timeLimitMs: item.timeLimitMs, memoryLimitMb: item.memoryLimitMb },
    ]),
  ) as ProblemDetail["limitsByLanguage"];
  for (const language of ["java", "cpp", "python"] as const) {
    if (!byLanguage[language])
      throw new ApiError("UNKNOWN_ERROR", 503, "Missing execution limits");
  }
  return byLanguage;
}

function toHistory(history: HistoryResponse): ProblemDetail["mySubmissions"] {
  return history.items.map((item) => ({
    id: item.submissionId,
    verdict: overallVerdict[item.status] ?? "systemError",
    language: item.language.toLowerCase() as SubmissionLanguage,
    passedCount: item.passedCount ?? 0,
    totalCount: item.totalCount ?? 0,
    runtimeMs: item.runtimeMs,
    memoryKb: item.memoryKb,
    submittedAt: item.submittedAt,
  }));
}

type OverviewResponse = {
  problemId: string;
  code: string;
  title: string;
  levelCode: string;
  statementMd: string;
  constraintsMd: string;
  topicNames: string[];
  acRate: number | null;
  hiddenTestcaseCount: number;
  bookmarked: boolean;
  examples: {
    input: string;
    output: string;
    explanationMd: string | null;
  }[];
};

/**
 * USR0104 (#6 + F4 limits and own history). One 404 PRB-010 from any call reads as "not found"; any
 * other failure (including a 503 from the F4 side) fails the whole page, it never shows invented limits.
 */
export async function fetchOverviewProblem(
  problemId: string,
): Promise<ProblemDetail | null> {
  try {
    const [overview, limits, history] = await Promise.all([
      api.get<OverviewResponse>(
        `/problem-bank/problems/${encodeURIComponent(problemId)}/overview`,
      ),
      getLimits(problemId),
      getHistory(problemId),
    ]);
    return {
      id: overview.problemId,
      code: overview.code,
      title: overview.title,
      difficulty: overview.levelCode,
      topics: overview.topicNames,
      // The overview does not carry the wrapper flag and the page does not read it.
      submissionModel: "both",
      statementMd: [overview.statementMd, overview.constraintsMd]
        .filter(Boolean)
        .join("\n\n"),
      examples: overview.examples.map((item) => ({
        input: item.input,
        output: item.output,
        explanation: item.explanationMd ?? undefined,
      })),
      limitsByLanguage: toLimits(limits),
      acRate: overview.acRate,
      saved: overview.bookmarked,
      hiddenTestcaseCount: overview.hiddenTestcaseCount,
      starterCode: {} as Record<StarterCodeKey, string>,
      sampleTestcases: [],
      mySubmissions: toHistory(history),
      solutionReview: null,
    };
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

export async function fetchWorkspaceProblem(
  problemId: string,
): Promise<ProblemDetail | null> {
  const path = `/problem-bank/problems/${encodeURIComponent(problemId)}`;
  try {
    const [workspace, overview, samples, limits, history] = await Promise.all([
      api.get<WorkspaceResponse>(`${path}/workspace`),
      api.get<{ hiddenTestcaseCount: number }>(`${path}/overview`),
      api.get<{
        items: {
          testcaseId: string;
          input: string | null;
          expectedOutput: string | null;
          inputDownloadUrl: string | null;
          expectedOutputDownloadUrl: string | null;
        }[];
      }>(`${path}/sample-testcases`),
      getLimits(problemId),
      getHistory(problemId),
    ]);
    return {
      id: workspace.id,
      code: workspace.code,
      title: workspace.title,
      difficulty: workspace.levelCode,
      topics: workspace.topicNames,
      submissionModel: workspace.functionWrapperSupported
        ? "both"
        : "stdioOnly",
      statementMd: [workspace.statementMd, workspace.constraintsMd]
        .filter(Boolean)
        .join("\n\n"),
      examples: workspace.workedExamples.map((item) => ({
        input: item.input,
        output: item.output,
        explanation: item.explanationMd ?? undefined,
      })),
      limitsByLanguage: toLimits(limits),
      acRate: workspace.acRate,
      saved: workspace.bookmarked,
      hiddenTestcaseCount: overview.hiddenTestcaseCount,
      starterCode: Object.fromEntries(
        Object.keys(languageCodes).flatMap((language) =>
          ["function", "stdio"].map((mode) => [`${language}:${mode}`, ""]),
        ),
      ) as Record<StarterCodeKey, string>,
      sampleTestcases: await Promise.all(
        samples.items.map(async (item) => ({
          testcaseId: item.testcaseId,
          input: await sampleText(item.input, item.inputDownloadUrl),
          expectedOutput: await sampleText(
            item.expectedOutput,
            item.expectedOutputDownloadUrl,
          ),
        })),
      ),
      mySubmissions: toHistory(history),
      solutionReview: null,
    };
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

export function useWorkspaceProblem(problemId: string | undefined) {
  return useQuery({
    queryKey: ["problem", "workspace", problemId],
    queryFn: () =>
      withMockData(
        () => fetchProblemDetail(problemId!) ?? null,
        () => fetchWorkspaceProblem(problemId!),
      ),
    enabled: Boolean(problemId),
    staleTime: 0,
    retry: false,
  });
}
