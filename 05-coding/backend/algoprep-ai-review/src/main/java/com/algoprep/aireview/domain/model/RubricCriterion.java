package com.algoprep.aireview.domain.model;

/**
 * The four fixed rubric criteria in their fixed order, with their weights (BR-AI-09, application
 * constants; not stored and not admin-configurable, DEC-2026-1014-drop-admin-rubric-config).
 */
public enum RubricCriterion {
  CLARITY(25),
  TECHNICAL_ACCURACY(30),
  PUSHBACK_HANDLING(25),
  COMPLEXITY_AWARENESS(20);

  private final int weightPercent;

  RubricCriterion(int weightPercent) {
    this.weightPercent = weightPercent;
  }

  public int weightPercent() {
    return weightPercent;
  }
}
