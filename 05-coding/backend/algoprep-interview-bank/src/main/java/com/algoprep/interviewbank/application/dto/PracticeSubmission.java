package com.algoprep.interviewbank.application.dto;

import com.algoprep.interviewbank.domain.model.LearnerCategory;
import java.util.UUID;

/** The trusted caller (from the token) plus the one untrusted field, the answer text. */
public record PracticeSubmission(
    UUID userId, LearnerCategory category, UUID questionId, String answerText) {
  @Override
  public String toString() {
    return "PracticeSubmission[user=" + userId + ", question=" + questionId + "]";
  }
}
