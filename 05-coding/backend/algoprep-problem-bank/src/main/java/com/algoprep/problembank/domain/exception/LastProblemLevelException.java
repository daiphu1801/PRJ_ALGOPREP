package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.Map;

public final class LastProblemLevelException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;

  public LastProblemLevelException() {
    super("PRB-033", "At least one problem level must remain");
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("reason", "LAST_LEVEL");
  }
}
