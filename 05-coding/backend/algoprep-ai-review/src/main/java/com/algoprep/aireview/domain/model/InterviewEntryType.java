package com.algoprep.aireview.domain.model;

/** How a mock-interview session starts (F5-24). */
public enum InterviewEntryType {
  SUBMISSION,
  QUESTION_BANK,
  TOPIC_SELECTION;

  /** Self-practice entries carry the F5-28 settings; the submission entry stores them as NULL. */
  public boolean selfPractice() {
    return this != SUBMISSION;
  }
}
