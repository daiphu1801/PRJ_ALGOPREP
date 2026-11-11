package com.algoprep.aireview.domain.model;

import java.util.UUID;

/**
 * F5 view of an ACCEPTED submission (DD F5 section 32.4). There is deliberately no field for a
 * testcase, expected output, actual output, diff or stderr.
 */
public record SubmissionSnapshot(
    UUID submissionId, UUID userId, UUID problemId, String language, String sourceCode) {
  @Override
  public String toString() {
    return "SubmissionSnapshot[submissionId=" + submissionId + ", sourceCode=REDACTED]";
  }
}
