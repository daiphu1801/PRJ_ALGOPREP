import { beforeEach, describe, expect, it, vi } from "vitest";
const post = vi.fn();
vi.mock("@/shared/api", () => ({
  api: { post: (...args: unknown[]) => post(...args) },
}));
import {
  runRealSamples,
  submitRealCode,
  type ExecutionInput,
} from "./real-execution";
const input: ExecutionInput = {
  problemId: "p",
  language: "python",
  mode: "function",
  sourceCode: "def solve():\n  return 42",
  inputMethod: "type",
};
beforeEach(() => {
  post.mockReset();
});
describe("real execution", () => {
  it("sends source once and returns a pending ticket rather than a simulated verdict", async () => {
    post.mockResolvedValue({
      submissionId: "s",
      status: "PENDING",
      totalCountHint: 8,
    });
    expect(await submitRealCode(input)).toMatchObject({
      submissionId: "s",
      overallVerdict: "pending",
      testcases: [],
    });
    expect(post).toHaveBeenCalledExactlyOnceWith(
      "/judge/problems/p/submissions",
      {
        language: "PYTHON",
        submissionModel: "FUNCTION_WRAPPER",
        sourceCode: input.sourceCode,
        sourceInputMethod: "EDITOR",
      },
    );
  });
  it("sends the uploaded source and filename in the JSON contract", async () => {
    post.mockResolvedValue({
      submissionId: "s",
      status: "COMPILING",
      totalCountHint: null,
    });
    await submitRealCode({
      ...input,
      language: "cpp",
      mode: "stdio",
      inputMethod: "upload",
      fileName: "main.cpp",
    });
    expect(post.mock.calls[0]?.[1]).toMatchObject({
      language: "CPP",
      submissionModel: "STANDARD_IO",
      sourceInputMethod: "FILE_UPLOAD",
      fileName: "main.cpp",
    });
  });
  it("preserves compile diagnostics without claiming an empty sample list passed", async () => {
    post.mockResolvedValue({
      outcome: "COMPILE_ERROR",
      compileErrorMessage: "Line 2: invalid syntax",
      results: [],
    });
    expect(await runRealSamples({ ...input, samples: [] })).toEqual({
      compileErrorMessage: "Line 2: invalid syntax",
      cases: [],
    });
  });
  it("does not retry admission on an ambiguous infrastructure error", async () => {
    const error = new Error("unavailable");
    post.mockRejectedValue(error);
    await expect(submitRealCode(input)).rejects.toBe(error);
    expect(post).toHaveBeenCalledTimes(1);
  });
});
