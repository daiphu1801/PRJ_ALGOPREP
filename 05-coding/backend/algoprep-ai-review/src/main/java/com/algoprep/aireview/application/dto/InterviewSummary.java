package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.RubricCriterion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Endpoint 13 payload; {@code averageScore} and {@code weakest} are null with no session. */
public record InterviewSummary(
    int completedSessionCount,
    BigDecimal averageScore,
    RubricCriterion weakest,
    List<Recent> recent) {
  public InterviewSummary {
    recent = List.copyOf(recent);
  }

  /** One recent COMPLETED session with its four scores. */
  public record Recent(
      UUID sessionId,
      InterviewEntryType entryType,
      Instant startedAt,
      Instant endedAt,
      BigDecimal overallScore,
      Map<RubricCriterion, BigDecimal> rubric) {}
}
