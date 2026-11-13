package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-115: hints are not enabled for this session (hint_allowed is false or NULL). */
public final class AiHintNotAllowedException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-115";

  public AiHintNotAllowedException() {
    super(CODE, "Hints are not allowed in this session");
  }
}
