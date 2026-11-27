package com.algoprep.judge.domain.exception;

/** Raw SQL exceptions may include the submitted source; they must not cross this boundary. */
public final class SubmissionAdmissionUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public SubmissionAdmissionUnavailableException() {
    super("Submission admission is unavailable");
  }
}
