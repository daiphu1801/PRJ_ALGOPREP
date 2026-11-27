package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.InvalidRequestException;
import java.util.List;
import java.util.Map;

/** Invalid content field (PRB-001, HTTP 400); metadata never includes field values or source. */
public final class ProblemContentInvalidException extends InvalidRequestException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final String field;
  private final String rule;

  public ProblemContentInvalidException(String field, String rule) {
    super("PRB-001", "Invalid problem content");
    this.field = field;
    this.rule = rule;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("fields", List.of(Map.of("field", field, "rule", rule)));
  }
}
