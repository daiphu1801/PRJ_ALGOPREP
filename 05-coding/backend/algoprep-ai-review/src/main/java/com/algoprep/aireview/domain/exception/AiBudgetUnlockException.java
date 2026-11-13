package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-203: unlocking while the period total has already reached the limit. */
public final class AiBudgetUnlockException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-203";

  public AiBudgetUnlockException() {
    super(CODE, "Raise the limit before unlocking the budget");
  }
}
