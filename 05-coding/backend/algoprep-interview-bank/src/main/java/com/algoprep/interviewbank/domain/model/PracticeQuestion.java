package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/** The in-memory snapshot read once per submission: prompt, validation and the stored rubric. */
public record PracticeQuestion(
    UUID id, String title, String contentMarkdown, List<RubricCriterion> rubric) {
  public PracticeQuestion {
    rubric = List.copyOf(rubric);
  }
}
