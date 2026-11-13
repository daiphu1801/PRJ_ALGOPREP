package com.algoprep.common.exception;

/** The caller exceeded a request limit. Maps to HTTP 429. */
public class RateLimitExceededException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  public RateLimitExceededException(String errorCode, String message) {
    super(errorCode, message);
  }
}
