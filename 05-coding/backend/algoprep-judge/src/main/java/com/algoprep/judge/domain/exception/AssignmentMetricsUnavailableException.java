package com.algoprep.judge.domain.exception;

/** The calling module owns HTTP/degraded mapping; no HTTP code or dependency detail here. */
public final class AssignmentMetricsUnavailableException extends RuntimeException {
  public AssignmentMetricsUnavailableException() {
    super("Assignment metrics unavailable");
  }
}
