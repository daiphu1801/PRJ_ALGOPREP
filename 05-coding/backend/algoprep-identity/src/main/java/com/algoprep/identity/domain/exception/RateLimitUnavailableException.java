package com.algoprep.identity.domain.exception;

import com.algoprep.common.exception.ServiceUnavailableException;

/** A required password-attempt limiter could not be checked or updated (HTTP 503 IDT-510). */
public final class RateLimitUnavailableException extends ServiceUnavailableException {
  private static final long serialVersionUID = 1L;

  public RateLimitUnavailableException() {
    super("IDT-510", "Password attempt protection is temporarily unavailable; retry later", null);
  }
}
