package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceNotFoundException;

/** AIR-121: the price row does not exist (also thrown for a non-UUID id). */
public final class AiModelPriceNotFoundException extends ResourceNotFoundException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-121";

  public AiModelPriceNotFoundException() {
    super(CODE, "Model price not found");
  }
}
