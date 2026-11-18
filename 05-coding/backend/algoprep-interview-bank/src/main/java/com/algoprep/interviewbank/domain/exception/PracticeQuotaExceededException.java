package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.RateLimitExceededException;
import java.util.Map;

/** INB-041: more than 20 practice attempts in the current hour; nothing was stored. */
public final class PracticeQuotaExceededException extends RateLimitExceededException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final long retryAfterSeconds;

  public PracticeQuotaExceededException(long retryAfterSeconds) {
    super("INB-041", "Hourly practice limit reached");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("retryAfterSeconds", retryAfterSeconds);
  }
}
