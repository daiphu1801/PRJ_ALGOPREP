package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.RateLimitExceededException;

/** INB-042: the shared AI token budget is locked (maps AIR-401); nothing was stored. */
public final class PracticeBudgetLockedException extends RateLimitExceededException {
  private static final long serialVersionUID = 1L;

  public PracticeBudgetLockedException() {
    super("INB-042", "AI token budget is locked");
  }
}
