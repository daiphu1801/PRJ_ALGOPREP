package com.algoprep.interviewbank.domain.exception;

/** The AI answered with a result that does not satisfy the rubric snapshot (INB-502). */
public final class FeedbackInvalidException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public FeedbackInvalidException(Throwable cause) {
    super("AI feedback does not match the rubric", cause);
  }
}
