package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.RateLimitExceededException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AIR-402 (hourly review quota), AIR-403 (hourly interview quota) or AIR-404 (cooldown). Carries
 * {@code retryAfterSeconds}, which the advice also copies into the {@code Retry-After} header, and
 * {@code limit} for AIR-402.
 */
public final class AiRateLimitedException extends RateLimitExceededException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  public static final String QUOTA = "AIR-402";
  public static final String INTERVIEW_QUOTA = "AIR-403";
  public static final String COOLDOWN = "AIR-404";

  private final long retryAfterSeconds;
  private final Integer limit;

  private AiRateLimitedException(String code, String message, long retryAfter, Integer limit) {
    super(code, message);
    this.retryAfterSeconds = retryAfter;
    this.limit = limit;
  }

  public static AiRateLimitedException quota(int limit, long retryAfterSeconds) {
    return new AiRateLimitedException(
        QUOTA, "Hourly review quota exceeded", retryAfterSeconds, limit);
  }

  public static AiRateLimitedException interviewQuota(int limit, long retryAfterSeconds) {
    return new AiRateLimitedException(
        INTERVIEW_QUOTA, "Hourly interview quota exceeded", retryAfterSeconds, limit);
  }

  public static AiRateLimitedException cooldown(long retryAfterSeconds) {
    return new AiRateLimitedException(COOLDOWN, "Still in cooldown", retryAfterSeconds, null);
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    Map<String, Object> details = new LinkedHashMap<>();
    if (limit != null) {
      details.put("limit", limit);
    }
    details.put("retryAfterSeconds", retryAfterSeconds);
    return details;
  }
}
