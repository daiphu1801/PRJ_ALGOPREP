package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.SpecEvaluation;
import java.util.List;
import java.util.Map;

/** Hard P5/JSON-shape failures (HTTP 422 PRB-036); contains no user source or testcase data. */
public final class ProblemSpecInvalidException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final List<SpecEvaluation.Violation> violations;

  public ProblemSpecInvalidException(List<SpecEvaluation.Violation> violations) {
    super("PRB-036", "Invalid problem specification");
    this.violations = List.copyOf(violations);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("violations", violations);
  }
}
