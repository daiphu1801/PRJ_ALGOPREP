package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Internal snapshot, never serialized directly. */
public record SubmissionReadState(
    UUID id,
    UUID userId,
    UUID problemId,
    String language,
    String model,
    String status,
    Instant submittedAt,
    Instant finishedAt,
    String source,
    String compileError,
    Integer passed,
    Integer total,
    BigDecimal score,
    Integer runtime,
    Integer memory,
    boolean expired) {
  public boolean terminal() {
    return !java.util.Set.of("PENDING", "COMPILING", "JUDGING").contains(status);
  }

  public boolean scored() {
    return terminal() && !status.equals("COMPILE_ERROR") && !status.equals("SYSTEM_ERROR");
  }

  @Override
  public String toString() {
    return "SubmissionReadState[redacted]";
  }

  public record Case(
      UUID id,
      int order,
      String verdict,
      int runtime,
      int memory,
      boolean sample,
      String actual,
      String stderr) {
    @Override
    public String toString() {
      return "SubmissionCase[redacted]";
    }
  }

  public record Beats(long total, long slower) {}
}
