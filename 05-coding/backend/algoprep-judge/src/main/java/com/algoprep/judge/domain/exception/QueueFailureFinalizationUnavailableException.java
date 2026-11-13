package com.algoprep.judge.domain.exception;

/** Fixed message without SQL, event body, submitted source or nested driver exception. */
public final class QueueFailureFinalizationUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public QueueFailureFinalizationUnavailableException() {
    super("Submission queue failure could not be resolved");
  }
}
