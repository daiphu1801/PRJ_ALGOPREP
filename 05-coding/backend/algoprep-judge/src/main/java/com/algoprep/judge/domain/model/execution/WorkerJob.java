package com.algoprep.judge.domain.model;

import java.util.UUID;

/** Source is trusted DB input and must never enter logs or broker messages. */
public record WorkerJob(
    UUID id,
    UUID problemId,
    String language,
    String model,
    String sourceCode,
    int testcaseVersion,
    String status,
    boolean hasResults) {
  @Override
  public String toString() {
    return "WorkerJob[id=" + id + ", status=" + status + "]";
  }
}
