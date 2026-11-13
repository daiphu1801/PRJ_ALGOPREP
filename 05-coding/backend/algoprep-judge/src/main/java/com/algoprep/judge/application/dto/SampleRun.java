package com.algoprep.judge.application.dto;

import com.algoprep.judge.application.dto.ExecutionModels.TestcaseResult;
import java.util.List;
import java.util.UUID;

/** Synchronous sample contracts; source never appears in diagnostic strings. */
public final class SampleRun {
  private SampleRun() {}

  public record Command(
      UUID userId, UUID problemId, String language, String submissionModel, String sourceCode) {
    @Override
    public String toString() {
      return "SampleRun.Command[redacted]";
    }
  }

  public record Preparation(Command command, UUID requestId, int specVersion) {
    @Override
    public String toString() {
      return "SampleRun.Preparation[redacted]";
    }
  }

  public record Reservation(boolean recorded, long retryAfterSeconds) {}

  public record Result(
      String outcome,
      String compileErrorMessage,
      int passedCount,
      int totalCount,
      List<Case> results) {
    public Result {
      results = List.copyOf(results);
    }

    @Override
    public String toString() {
      return "SampleRun.Result[redacted]";
    }
  }

  public record Case(
      UUID testcaseId,
      int order,
      ExecutionModels.Verdict verdict,
      long runtimeMs,
      long memoryKb,
      String actualOutput,
      String stderrSnippet) {
    public static Case of(UUID id, TestcaseResult r) {
      return new Case(
          id,
          r.order(),
          r.verdict(),
          r.runtimeMs(),
          r.memoryKb(),
          r.actualOutputSnippet(),
          r.stderrSnippet());
    }

    @Override
    public String toString() {
      return "SampleRun.Case[redacted]";
    }
  }
}
