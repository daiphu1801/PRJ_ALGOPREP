package com.algoprep.common.exception;

/** The request body is larger than the endpoint accepts. Maps to HTTP 413. */
public class PayloadTooLargeException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  public PayloadTooLargeException(String errorCode, String message) {
    super(errorCode, message);
  }
}
