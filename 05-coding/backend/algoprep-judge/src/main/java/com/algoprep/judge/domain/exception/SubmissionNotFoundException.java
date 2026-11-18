package com.algoprep.judge.domain.exception;

public final class SubmissionNotFoundException extends RuntimeException {
  public SubmissionNotFoundException() {
    super("Submission not found");
  }
}
