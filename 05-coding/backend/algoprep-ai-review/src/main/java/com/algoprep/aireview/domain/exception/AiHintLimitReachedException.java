package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-117: hint_count reached {@code ai.interview.max-hints-per-session}. */
public final class AiHintLimitReachedException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-117";

  public AiHintLimitReachedException() {
    super(CODE, "Hint limit for this session reached");
  }
}
