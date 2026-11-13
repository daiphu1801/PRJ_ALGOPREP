package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.RateLimitExceededException;
import java.util.Map;

/** 429 PRB-055: ten sample-solution runs in a sliding 60 seconds. */
public final class ReferenceRunRateLimitedException extends RateLimitExceededException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final long retryAfterSeconds;

  public ReferenceRunRateLimitedException(long retryAfterSeconds) {
    super("PRB-055", "Sample solution run limit reached");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("retryAfterSeconds", retryAfterSeconds);
  }
}
