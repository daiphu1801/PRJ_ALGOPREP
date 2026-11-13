package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceNotFoundException;

/** AIR-120: the anomaly alert does not exist (also thrown for a non-UUID id). */
public final class AiUsageAlertNotFoundException extends ResourceNotFoundException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-120";

  public AiUsageAlertNotFoundException() {
    super(CODE, "Usage alert not found");
  }
}
