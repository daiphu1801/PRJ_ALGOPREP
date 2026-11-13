package com.algoprep.interviewbank.domain.exception;

/** Unique (user, question, attempt_no) was violated by a parallel submission; retry the write. */
public final class AttemptNumberConflictException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public AttemptNumberConflictException(Throwable cause) {
    super("attempt number taken by a parallel submission", cause);
  }
}
