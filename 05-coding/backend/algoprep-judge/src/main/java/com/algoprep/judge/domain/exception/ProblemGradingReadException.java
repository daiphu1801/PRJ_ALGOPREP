package com.algoprep.judge.domain.exception;

/** The future worker can distinguish invalid pinned data from a retryable infrastructure fault. */
public final class ProblemGradingReadException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final Reason reason;

  public enum Reason {
    SPEC_UNAVAILABLE,
    SANDBOX_FAULT
  }

  public ProblemGradingReadException(Reason reason) {
    super("Problem grading data is unavailable");
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
