package com.algoprep.common.exception;

/**
 * The caller could not be authenticated: wrong credentials, or a refresh token that is invalid,
 * expired or reused. Maps to HTTP 401.
 */
public class UnauthenticatedException extends AlgoPrepException {

  private static final long serialVersionUID = 1L;

  public UnauthenticatedException(String errorCode, String message) {
    super(errorCode, message);
  }
}
