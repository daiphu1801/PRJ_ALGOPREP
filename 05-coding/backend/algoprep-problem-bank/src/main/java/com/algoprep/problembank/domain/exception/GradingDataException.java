package com.algoprep.problembank.domain.exception;

/** Typed internal failure; never retains SQL, object keys, hidden bytes or upstream causes. */
public final class GradingDataException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final Kind kind;

  public enum Kind {
    SPEC_UNAVAILABLE,
    INFRASTRUCTURE
  }

  public GradingDataException(Kind kind) {
    super("Problem grading data is unavailable");
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
