package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.List;
import java.util.Map;

/** Publishing refused: lists every unmet condition, not only the first (PRB-030, HTTP 422). */
public final class PublishChecklistFailedException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final transient List<Map<String, Object>> items;

  public PublishChecklistFailedException(List<Map<String, Object>> items) {
    super("PRB-030", "Publish checklist is not satisfied");
    this.items = List.copyOf(items);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("items", items);
  }
}
