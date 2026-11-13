package com.algoprep.problembank.domain.exception;

/** Signals corrupt persisted JSON to trusted readers without retaining the raw parser cause. */
public final class UnreadableProblemSpecException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UnreadableProblemSpecException() {
    super("Stored function specification is invalid");
  }
}
