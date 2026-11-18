package com.algoprep.interviewbank.domain.exception;

/** The AI side cannot serve (provider, timeout or gate failure); the attempt is stored FAILED. */
public final class FeedbackUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public FeedbackUnavailableException(Throwable cause) {
    super("AI feedback unavailable", cause);
  }
}
