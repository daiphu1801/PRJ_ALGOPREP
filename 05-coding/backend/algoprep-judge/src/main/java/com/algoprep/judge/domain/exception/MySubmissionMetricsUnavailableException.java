package com.algoprep.judge.domain.exception;

public final class MySubmissionMetricsUnavailableException extends RuntimeException {
  public MySubmissionMetricsUnavailableException() {
    super("Submission metrics are temporarily unavailable");
  }
}
