package com.algoprep.judge.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Internal persisted ticket; it is not proof that the broker has accepted a message. */
public record PendingSubmissionTicket(
    UUID submissionId, ProgramType programType, Instant submittedAt, int totalCountHint) {
  public String status() {
    return "PENDING";
  }

  public record ProgramType(String language, String submissionModel) {}
}
