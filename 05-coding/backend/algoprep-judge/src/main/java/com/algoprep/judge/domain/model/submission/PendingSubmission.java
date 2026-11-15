package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Admission data only; worker settings and spec version are deliberately read later. */
public record PendingSubmission(
    UUID submissionId, Owner owner, Program program, int testcaseSetVersion, Instant submittedAt) {
  public record Owner(UUID userId, UUID problemId) {}

  public record Program(
      String language, String submissionModel, String sourceCode, String sourceInputMethod) {
    @Override
    public String toString() {
      return "Program[language=" + language + ", submissionModel=" + submissionModel + "]";
    }
  }
}
