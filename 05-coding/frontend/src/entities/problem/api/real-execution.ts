import { api } from "@/shared/api";
import type {
  InputMethod,
  OverallVerdict,
  SampleRunResult,
  SubmissionLanguage,
  SubmissionRunResult,
  WrapperMode,
} from "../model/types";

export type ExecutionInput = {
  problemId: string;
  language: SubmissionLanguage;
  mode: WrapperMode;
  sourceCode: string;
  inputMethod: InputMethod;
  fileName?: string;
};
export const overallVerdict: Record<string, OverallVerdict> = {
  PENDING: "pending",
  COMPILING: "compiling",
  JUDGING: "running",
  ACCEPTED: "accepted",
  WRONG_ANSWER: "wrongAnswer",
  TIME_LIMIT_EXCEEDED: "timeLimitExceeded",
  RUNTIME_ERROR: "runtimeError",
  COMPILE_ERROR: "compileError",
  SYSTEM_ERROR: "systemError",
};
function body(input: ExecutionInput) {
  return {
    language: input.language.toUpperCase(),
    submissionModel:
      input.mode === "function" ? "FUNCTION_WRAPPER" : "STANDARD_IO",
    sourceCode: input.sourceCode,
  };
}
export async function runRealSamples(
  input: ExecutionInput & {
    samples: { testcaseId?: string; input: string; expectedOutput: string }[];
  },
): Promise<SampleRunResult> {
  const response = await api.post<{
    outcome: string;
    compileErrorMessage: string | null;
    results: {
      testcaseId: string;
      order: number;
      verdict: string;
      actualOutput: string;
      runtimeMs: number;
      memoryKb: number;
    }[];
  }>(
    `/judge/problems/${encodeURIComponent(input.problemId)}/run-sample`,
    body(input),
  );
  return {
    compileErrorMessage: response.compileErrorMessage,
    cases: response.results.map((item) => ({
      input:
        input.samples.find((sample) => sample.testcaseId === item.testcaseId)
          ?.input ??
        input.samples[item.order - 1]?.input ??
        "",
      expectedOutput:
        input.samples.find((sample) => sample.testcaseId === item.testcaseId)
          ?.expectedOutput ??
        input.samples[item.order - 1]?.expectedOutput ??
        "",
      actualOutput: item.actualOutput,
      passed: item.verdict === "AC",
      runtimeMs: item.runtimeMs,
      memoryKb: item.memoryKb,
    })),
  };
}
export async function submitRealCode(
  input: ExecutionInput,
): Promise<SubmissionRunResult> {
  const response = await api.post<{
    submissionId: string;
    status: string;
    totalCountHint: number | null;
  }>(`/judge/problems/${encodeURIComponent(input.problemId)}/submissions`, {
    ...body(input),
    sourceInputMethod:
      input.inputMethod === "upload" ? "FILE_UPLOAD" : "EDITOR",
    ...(input.inputMethod === "upload" ? { fileName: input.fileName } : {}),
  });
  return {
    submissionId: response.submissionId,
    overallVerdict: overallVerdict[response.status] ?? "pending",
    passedCount: 0,
    totalCount: response.totalCountHint ?? 0,
    testcases: [],
    runtimeMs: 0,
    memoryKb: 0,
  };
}
