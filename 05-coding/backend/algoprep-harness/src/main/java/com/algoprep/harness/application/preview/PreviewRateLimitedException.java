package com.algoprep.harness.application.preview;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.RateLimitExceededException;
import java.util.Map;

/** HRN-061: the author called P6 more than 10 times in one second. */
public final class PreviewRateLimitedException extends RateLimitExceededException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final long retryAfterSeconds;

  public PreviewRateLimitedException(long retryAfterSeconds) {
    super("HRN-061", "Too many signature previews");
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("retryAfterSeconds", retryAfterSeconds);
  }
}
