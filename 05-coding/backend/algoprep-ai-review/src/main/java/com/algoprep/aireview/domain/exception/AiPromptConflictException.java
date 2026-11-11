package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-201: the ACTIVE prompt is no longer the one the admin screen loaded. */
public final class AiPromptConflictException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-201";

  public AiPromptConflictException() {
    super(CODE, "Prompt configuration was changed by someone else");
  }
}
