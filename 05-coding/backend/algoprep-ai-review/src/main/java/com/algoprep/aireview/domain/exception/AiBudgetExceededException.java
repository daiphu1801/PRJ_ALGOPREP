package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.RateLimitExceededException;

/** AIR-401: the monthly token budget is locked for STUDENT and INSTRUCTOR. Never retried. */
public final class AiBudgetExceededException extends RateLimitExceededException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-401";

  public AiBudgetExceededException() {
    super(CODE, "AI token budget is exhausted");
  }
}
