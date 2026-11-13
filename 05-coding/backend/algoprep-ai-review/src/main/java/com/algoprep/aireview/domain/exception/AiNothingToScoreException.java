package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-116: the learner has not answered any turn yet, so there is nothing to score. */
public final class AiNothingToScoreException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-116";

  public AiNothingToScoreException() {
    super(CODE, "The session has no answered turn to score");
  }
}
