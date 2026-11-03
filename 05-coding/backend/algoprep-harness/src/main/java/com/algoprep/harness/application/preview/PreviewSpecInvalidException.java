package com.algoprep.harness.application.preview;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.SpecEvaluation.Violation;
import java.util.List;
import java.util.Map;

/** HRN-060: structured P5 violations, with no draft content in the log message. */
public final class PreviewSpecInvalidException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final List<Violation> violations;

  public PreviewSpecInvalidException(List<Violation> violations) {
    super("HRN-060", "Invalid function signature draft");
    this.violations = List.copyOf(violations);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("violations", violations);
  }
}
