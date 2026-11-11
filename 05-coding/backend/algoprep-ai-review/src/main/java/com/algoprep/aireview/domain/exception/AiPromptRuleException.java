package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-202 (model outside the allowed list) or AIR-205 (publish without any real change). */
public final class AiPromptRuleException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String MODEL_NOT_ALLOWED = "AIR-202";
  public static final String NOTHING_CHANGED = "AIR-205";

  public AiPromptRuleException(String code, String message) {
    super(code, message);
  }
}
