package com.algoprep.aireview.domain.llm;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException.Reason;

/** Maps an HTTP status or a timeout to the AIR-501 {@code details.reason} (DD F5 section 34). */
public final class ProviderFailures {
  private ProviderFailures() {}

  /**
   * @param httpStatus status of the provider answer, or null when no answer arrived
   * @param timedOut true when the cause chain holds a connect, read or deadline timeout
   */
  public static Reason classify(Integer httpStatus, boolean timedOut) {
    if (timedOut || (httpStatus != null && (httpStatus == 408 || httpStatus == 504))) {
      return Reason.TIMEOUT;
    }
    // 429, other 5xx, 401/403 (bad key), 400 (bad model) and transport failures all read as the
    // provider being unusable right now.
    return Reason.PROVIDER_ERROR;
  }
}
