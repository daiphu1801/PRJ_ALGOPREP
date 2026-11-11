package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** One row of {@code ai.rubric_scores}: score 0.00 to 10.00 and a non-empty comment. */
public record RubricScore(RubricCriterion criterion, BigDecimal score, String comment) {
  public RubricScore {
    Objects.requireNonNull(criterion, "criterion");
    Objects.requireNonNull(score, "score");
    Objects.requireNonNull(comment, "comment");
  }
}
