package com.algoprep.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The caller's own submission history as the judge lists it: one line per submission, never the
 * source code and never testcase data.
 *
 * @param rows newest first
 * @param truncated true when the caller has more submissions than the row cap allowed
 */
public record SubmissionExport(List<Row> rows, boolean truncated) {
  public SubmissionExport {
    rows = List.copyOf(rows);
  }

  /**
   * One submission line. Problem fields are null when the problem-bank could not name the problem.
   */
  public record Row(
      UUID submissionId,
      Instant submittedAt,
      String problemCode,
      String problemTitle,
      String language,
      String status,
      Integer passedCount,
      Integer totalCount,
      Integer runtimeMs) {}
}
