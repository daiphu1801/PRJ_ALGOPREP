package com.algoprep.judge.domain.model;

import java.util.UUID;

/** Trusted internal snapshot; the AI caller must verify userId before using sourceCode. */
public record AcceptedSubmissionSnapshot(
    UUID submissionId,
    UUID userId,
    UUID problemId,
    String language,
    String sourceCode,
    Integer runtimeMs,
    Integer memoryKb) {
  @Override
  public String toString() {
    return "AcceptedSubmissionSnapshot[submissionId=" + submissionId + ", sourceCode=REDACTED]";
  }
}
