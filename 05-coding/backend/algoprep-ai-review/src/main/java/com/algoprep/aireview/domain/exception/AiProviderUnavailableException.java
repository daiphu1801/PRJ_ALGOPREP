package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.util.Map;

/** AIR-501: the LLM or an upstream port cannot serve the request (503, retryable). */
public final class AiProviderUnavailableException extends ServiceUnavailableException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-501";

  /** Values of {@code details.reason} (DD F5 section 34). */
  public enum Reason {
    TIMEOUT,
    PROVIDER_ERROR,
    UPSTREAM_PORT
  }

  private final Reason reason;

  public AiProviderUnavailableException(Reason reason, Throwable cause) {
    super(CODE, "AI provider or upstream port unavailable: " + reason, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("reason", reason.name());
  }
}
