package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-114: no answer turns are left (max turns or the last-stage ceiling is reached). */
public final class AiTurnLimitReachedException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-114";

  public AiTurnLimitReachedException() {
    super(CODE, "Interview session has no answer turns left");
  }
}
