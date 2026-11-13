package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * One attempt about to be inserted. The attempt number is not part of it: the writing transaction
 * numbers it after the question row is share-locked. {@code rubricSnapshot} is the rubric as it was
 * when the learner submitted.
 */
public record NewAttempt(
    UUID id,
    UUID userId,
    UUID questionId,
    String answerText,
    FeedbackStatus status,
    Feedback feedback,
    List<RubricCriterion> rubricSnapshot) {
  public NewAttempt {
    rubricSnapshot = List.copyOf(rubricSnapshot);
  }

  @Override
  public String toString() {
    return "NewAttempt[id=" + id + ", question=" + questionId + ", status=" + status + "]";
  }
}
