package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-113: another call (a turn or a hint) holds the session lock. */
public final class AiSessionBusyException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-113";

  public AiSessionBusyException() {
    super(CODE, "Interview session is processing another request");
  }
}
