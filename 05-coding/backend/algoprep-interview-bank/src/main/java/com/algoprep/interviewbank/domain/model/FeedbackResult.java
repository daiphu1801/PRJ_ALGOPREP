package com.algoprep.interviewbank.domain.model;

import java.util.List;

/** Feedback that already passed the schema check; codes are the exact snapshot strings. */
public record FeedbackResult(
    List<String> achievedPoints,
    List<String> missingPoints,
    List<String> improvementSuggestions,
    List<CriterionScore> criteria) {
  /** One score from the AI. */
  public record CriterionScore(String criterionCode, int achievementPercent) {}
}
