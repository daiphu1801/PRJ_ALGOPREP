package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Everything the AI side needs. The system instruction is the fixed server constant; the question
 * and the answer are data and never reach the system message.
 */
public record FeedbackRequest(
    UUID callId,
    UUID userId,
    UUID answerId,
    String systemInstruction,
    List<RubricCriterion> rubric,
    String questionTitle,
    String questionContent,
    String answerText) {
  public FeedbackRequest {
    rubric = List.copyOf(rubric);
  }

  @Override
  public String toString() {
    return "FeedbackRequest[answer=" + answerId + "]";
  }
}
