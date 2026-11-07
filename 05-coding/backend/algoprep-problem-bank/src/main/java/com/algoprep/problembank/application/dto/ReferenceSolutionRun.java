package com.algoprep.problembank.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Contract of the reference-solution run behind the publish checklist (BR-07, BR-15). The source
 * and the testcase bodies never appear in {@code toString}, so a log line cannot leak them.
 */
public final class ReferenceSolutionRun {
  private ReferenceSolutionRun() {}

  /** Limits as stored on the problem; the executor applies its own per-language scaling. */
  public record Limits(
      int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, int compileTimeoutSeconds) {}

  /** One approved testcase: stdin and the expected stdout. */
  public record Case(UUID testcaseId, String input, String expectedOutput) {
    @Override
    public String toString() {
      return "ReferenceSolutionRun.Case[" + testcaseId + "]";
    }
  }

  public record Request(
      UUID problemId,
      String language,
      boolean functionWrapper,
      String sourceCode,
      int specVersion,
      Limits limits,
      List<Case> cases) {
    public Request {
      cases = List.copyOf(cases);
    }

    @Override
    public String toString() {
      return "ReferenceSolutionRun.Request[" + problemId + "]";
    }
  }

  /** {@code verdict} is the executor's word for the case, for example AC, WA, TLE or RE. */
  public record CaseResult(
      UUID testcaseId, boolean passed, String verdict, Long runtimeMs, Long memoryKb) {
    /** For callers that only need the verdict, such as the publish gate. */
    public CaseResult(UUID testcaseId, boolean passed, String verdict) {
      this(testcaseId, passed, verdict, null, null);
    }
  }

  /**
   * A compile error means no case ran; otherwise there is one result per request case. {@code
   * compileMessage} is the sanitized compiler text, null when there was no compile error.
   */
  public record Result(boolean compileError, List<CaseResult> results, String compileMessage) {
    public Result {
      results = List.copyOf(results);
    }

    public Result(boolean compileError, List<CaseResult> results) {
      this(compileError, results, null);
    }

    public boolean allPassed() {
      return !compileError && !results.isEmpty() && results.stream().allMatch(CaseResult::passed);
    }
  }
}
