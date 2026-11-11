package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceNotFoundException;

/** AIR-110: the interview session does not exist (also thrown for a non-UUID id). */
public final class AiSessionNotFoundException extends ResourceNotFoundException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-110";

  public AiSessionNotFoundException() {
    super(CODE, "Interview session not found");
  }
}
