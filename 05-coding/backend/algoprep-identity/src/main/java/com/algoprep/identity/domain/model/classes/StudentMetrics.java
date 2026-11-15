package com.algoprep.identity.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the judge knows about one student inside one class (DD api F4 P1): only submissions inside
 * the enrolment and assignment windows count. {@code acceptedRatio} and the weekly ratios are null
 * when there is no eligible submission; {@code assignments} lists the problems still assigned.
 */
public record StudentMetrics(
    UUID studentId,
    Instant lastSubmittedAt,
    Double acceptedRatio,
    int streakDays,
    List<WeekPoint> weeks,
    List<AssignmentOutcome> assignments) {
  public StudentMetrics {
    weeks = List.copyOf(weeks);
    assignments = List.copyOf(assignments);
  }

  /** One completed ISO week (Monday start, UTC+7), oldest first in {@link #weeks}. */
  public record WeekPoint(LocalDate weekStart, Double acceptedRatio) {}

  /** The best result of the student on one currently assigned problem, since it was assigned. */
  public record AssignmentOutcome(
      UUID problemId,
      Instant assignedAt,
      BigDecimal bestRatio,
      String bestVerdict,
      boolean accepted) {}
}
