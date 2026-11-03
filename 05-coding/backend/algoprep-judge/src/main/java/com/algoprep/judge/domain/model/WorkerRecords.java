package com.algoprep.judge.domain.model;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Trusted worker persistence values; no source, expected output or broker payload. */
public final class WorkerRecords {
  private WorkerRecords() {}

  public record State(
      UUID id,
      Owner owner,
      String language,
      String model,
      String status,
      Integer total,
      int passed,
      Integer runtime,
      Integer memory,
      Instant submittedAt,
      Instant updatedAt) {}

  public record Result(int order, String verdict, Integer runtime, Integer memory) {}

  public record Token(UUID id, UUID value) {
    @Override
    public String toString() {
      return "Token[redacted]";
    }
  }

  public record TestcaseWrite(
      UUID submissionId,
      UUID testcaseId,
      UUID tokenId,
      int order,
      boolean sample,
      String verdict,
      Integer runtime,
      Integer memory,
      String actualOutput,
      String stderr,
      boolean returnStderr) {
    public TestcaseWrite {
      Objects.requireNonNull(submissionId);
      Objects.requireNonNull(testcaseId);
      Objects.requireNonNull(tokenId);
      if (order < 1
          || order > Integer.MAX_VALUE - 3
          || verdict == null
          || !Set.of("AC", "WA", "TLE", "MLE", "RE").contains(verdict)
          || (runtime != null && runtime < 0)
          || (memory != null && memory < 0)) {
        throw new IllegalArgumentException("Invalid testcase result");
      }
    }

    @Override
    public String toString() {
      return "TestcaseWrite[submissionId=" + submissionId + ", order=" + order + "]";
    }
  }

  public enum FinishKind {
    COMPLETE,
    COMPILE_ERROR,
    SYSTEM_ERROR
  }

  public record FinishCommand(
      UUID submissionId, FinishKind kind, String failureReason, String compileMessage) {
    public FinishCommand {
      Objects.requireNonNull(submissionId);
      Objects.requireNonNull(kind);
      boolean system = kind == FinishKind.SYSTEM_ERROR;
      boolean compile = kind == FinishKind.COMPILE_ERROR;
      if ((system
              && (failureReason == null
                  || !Set.of(
                          "SANDBOX_FAULT",
                          "SPEC_UNAVAILABLE",
                          "WORKER_TIMEOUT",
                          "QUEUE_UNAVAILABLE")
                      .contains(failureReason)))
          || (!system && failureReason != null)
          || (compile && (compileMessage == null || compileMessage.isBlank()))
          || (!compile && compileMessage != null)) {
        throw new IllegalArgumentException("Invalid submission finalization");
      }
    }

    @Override
    public String toString() {
      return "FinishCommand[submissionId=" + submissionId + ", kind=" + kind + "]";
    }
  }

  public record Outcome(
      String status,
      int passed,
      Integer total,
      BigDecimal score,
      Integer runtime,
      Integer memory,
      String failureReason,
      String compileMessage,
      Integer firstFailingOrder) {
    @Override
    public String toString() {
      return "Outcome[status=" + status + "]";
    }
  }

  public record Finished(State state, Outcome outcome, Integer judgedCount) {}
}
