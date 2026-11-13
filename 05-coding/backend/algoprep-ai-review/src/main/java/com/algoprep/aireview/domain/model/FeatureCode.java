package com.algoprep.aireview.domain.model;

/** Columns {@code prompt_templates.feature_code} and {@code token_usage.feature_code}. */
public enum FeatureCode {
  SOLUTION_REVIEW,
  MOCK_INTERVIEW,
  GENERATE_TESTCASE,
  PRACTICE_FEEDBACK;

  /**
   * Hourly quota (BR-AI-04); only the first two also have a cooldown. PRACTICE_FEEDBACK takes its
   * limit from the F6 caller and has no cooldown.
   */
  public boolean hasHourlyQuota() {
    return this == SOLUTION_REVIEW || this == MOCK_INTERVIEW || this == PRACTICE_FEEDBACK;
  }
}
