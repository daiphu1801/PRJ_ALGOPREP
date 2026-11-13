package com.algoprep.judge.domain.exception;

/** F4 read API errors; JDG-506 was approved by the owner on 2026-11-04. */
public final class SubmissionReadErrorCodes {
  /** HTTP 503. No client retry may create a new submission. */
  public static final String UNAVAILABLE = "JDG-506";

  private SubmissionReadErrorCodes() {}
}
