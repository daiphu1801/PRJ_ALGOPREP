package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The rubric of a COMPLETED session (BR-AI-09 steps 8 and 9): exactly four criteria in the fixed
 * order. The overall score and the summary are derived on every read and never stored.
 */
public record InterviewResult(
    UUID sessionId, List<RubricScore> criteria, int turnCount, Instant endedAt) {
  public InterviewResult {
    criteria = List.copyOf(criteria);
    if (criteria.size() != RubricCriterion.values().length) {
      throw new IllegalArgumentException("a result needs all four criteria");
    }
  }

  /** Sum of score x weight / 100, rounded half up to two decimals. */
  public BigDecimal overallScore() {
    return overall(criteria);
  }

  public static BigDecimal overall(List<RubricScore> criteria) {
    BigDecimal sum = BigDecimal.ZERO;
    for (RubricScore s : criteria) {
      sum = sum.add(s.score().multiply(BigDecimal.valueOf(s.criterion().weightPercent())));
    }
    return sum.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }

  /** The four comments in the fixed order, one blank line apart. */
  public String feedbackSummary() {
    return criteria.stream().map(RubricScore::comment).collect(Collectors.joining("\n\n"));
  }
}
