package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-111: the session is COMPLETED or ABANDONED. */
public final class AiSessionNotRunningException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-111";

  public AiSessionNotRunningException() {
    super(CODE, "Interview session is no longer running");
  }
}
