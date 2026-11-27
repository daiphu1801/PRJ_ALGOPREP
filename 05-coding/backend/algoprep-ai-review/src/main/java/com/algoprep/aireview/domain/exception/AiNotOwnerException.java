package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.PermissionDeniedException;

/** AIR-302: the data exists but belongs to someone else (DD F5 section 1.2 returns 403). */
public final class AiNotOwnerException extends PermissionDeniedException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-302";

  public AiNotOwnerException() {
    super(CODE, "Caller does not own this data");
  }
}
