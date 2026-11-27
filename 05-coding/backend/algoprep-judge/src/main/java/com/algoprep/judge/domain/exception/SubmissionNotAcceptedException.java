package com.algoprep.judge.domain.exception;

public final class SubmissionNotAcceptedException extends RuntimeException {
  public SubmissionNotAcceptedException() {
    super("Submission not accepted");
  }
}
