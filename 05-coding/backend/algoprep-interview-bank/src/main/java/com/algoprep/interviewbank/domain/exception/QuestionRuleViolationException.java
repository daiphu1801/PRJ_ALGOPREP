package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.Map;

/** INB-033 (weight sum) or INB-035 (unknown topic or level), with machine-readable details. */
public final class QuestionRuleViolationException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Map<String, Object> details;

  public QuestionRuleViolationException(String code, String message, Map<String, Object> details) {
    super(code, message);
    this.details = Map.copyOf(details);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
