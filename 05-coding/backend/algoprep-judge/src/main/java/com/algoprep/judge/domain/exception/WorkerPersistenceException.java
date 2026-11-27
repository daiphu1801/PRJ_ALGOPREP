package com.algoprep.judge.domain.exception;

/** Fixed boundary: JDBC failures can contain source or testcase snippets. */
public final class WorkerPersistenceException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public WorkerPersistenceException() {
    super("Worker persistence is unavailable");
  }
}
