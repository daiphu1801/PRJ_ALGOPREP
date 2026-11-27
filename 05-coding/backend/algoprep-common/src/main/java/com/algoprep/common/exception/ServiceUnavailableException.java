package com.algoprep.common.exception;

/**
 * A step the request depends on failed and the whole change was rolled back, so the caller may try
 * again later. Maps to HTTP 503.
 */
public class ServiceUnavailableException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  public ServiceUnavailableException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
