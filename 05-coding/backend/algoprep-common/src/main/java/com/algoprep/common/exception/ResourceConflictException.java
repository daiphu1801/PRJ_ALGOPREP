package com.algoprep.common.exception;

/** A resource already exists or conflicts with the requested operation (HTTP 409). */
public class ResourceConflictException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;

  public ResourceConflictException(String errorCode, String message) {
    super(errorCode, message);
  }
}
