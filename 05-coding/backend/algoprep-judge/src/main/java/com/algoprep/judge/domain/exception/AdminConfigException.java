package com.algoprep.judge.domain.exception;

/** Stable DD codes for configuration validation and conflicts. */
public final class AdminConfigException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String errorCode;
  private final int status;

  public AdminConfigException(int status, String errorCode) {
    super("Invalid judge configuration");
    this.status = status;
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }

  public int status() {
    return status;
  }
}
