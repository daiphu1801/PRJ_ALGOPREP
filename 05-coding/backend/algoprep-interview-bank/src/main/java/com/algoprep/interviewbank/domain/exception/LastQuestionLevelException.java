package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.Map;

/** INB-032: the question form requires a difficulty, so at least one level must remain. */
public final class LastQuestionLevelException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;

  public LastQuestionLevelException() {
    super("INB-032", "At least one question level must remain");
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("reason", "LAST_LEVEL");
  }
}
