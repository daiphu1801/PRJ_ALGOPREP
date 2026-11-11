package com.algoprep.interviewbank.domain.model;

import java.util.List;

/** The qualitative feedback of one attempt. No overall score and no pass mark (DB6:139-141). */
public record Feedback(
    List<String> achievedPoints,
    List<String> missingPoints,
    List<String> improvementSuggestions,
    List<FeedbackCriterion> criteria) {
  public Feedback {
    achievedPoints = List.copyOf(achievedPoints);
    missingPoints = List.copyOf(missingPoints);
    improvementSuggestions = List.copyOf(improvementSuggestions);
    criteria = List.copyOf(criteria);
  }
}
