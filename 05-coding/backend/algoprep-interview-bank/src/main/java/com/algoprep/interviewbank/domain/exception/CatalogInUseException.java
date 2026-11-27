package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.Map;

/** INB-030 (topic) or INB-031 (level): some question, even a RETIRED one, still references it. */
public final class CatalogInUseException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Map<String, Object> details;

  public CatalogInUseException(String code, long count, boolean level) {
    super(code, "Catalogue item is referenced by questions");
    details =
        level
            ? Map.of("reason", "IN_USE", "referencingQuestionCount", count)
            : Map.of("referencingQuestionCount", count);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
