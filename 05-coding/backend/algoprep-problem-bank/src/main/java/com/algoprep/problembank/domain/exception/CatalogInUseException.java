package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.Map;

public final class CatalogInUseException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Map<String, Object> details;

  public CatalogInUseException(String code, long count, boolean level) {
    super(code, "Catalogue item is referenced");
    details =
        level
            ? Map.of("reason", "IN_USE", "referencingProblemCount", count)
            : Map.of("referencingProblemCount", count);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
