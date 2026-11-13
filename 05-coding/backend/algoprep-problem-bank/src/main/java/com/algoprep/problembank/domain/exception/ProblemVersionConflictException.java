package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.ResourceConflictException;
import java.time.Instant;
import java.util.Map;

/** Stale authoring timestamp (HTTP 409 PRB-020). */
public final class ProblemVersionConflictException extends ResourceConflictException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Instant current;

  public ProblemVersionConflictException(Instant current) {
    super("PRB-020", "Problem was updated; reload before saving");
    this.current = current;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("currentUpdatedAt", current);
  }
}
