package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-204: another model already has this name (case-insensitive). */
public final class AiModelPriceConflictException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-204";

  public AiModelPriceConflictException() {
    super(CODE, "A model with this name already exists");
  }
}
