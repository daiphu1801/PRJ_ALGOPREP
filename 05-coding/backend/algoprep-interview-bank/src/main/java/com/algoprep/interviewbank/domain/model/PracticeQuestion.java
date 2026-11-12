package com.algoprep.interviewbank.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * The in-memory snapshot read once per submission: prompt, validation and the stored rubric. The
 * content is optional in the bank (CSV import, admin create), so null becomes an empty string here
 * and the AI side never sees a null.
 */
public record PracticeQuestion(
    UUID id, String title, String contentMarkdown, List<RubricCriterion> rubric) {
  public PracticeQuestion {
    contentMarkdown = contentMarkdown == null ? "" : contentMarkdown;
    rubric = List.copyOf(rubric);
  }
}
