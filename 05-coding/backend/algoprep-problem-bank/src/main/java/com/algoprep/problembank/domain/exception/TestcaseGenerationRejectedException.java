package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.List;
import java.util.Map;

/** 422 PRB-041, PRB-042 and PRB-043 of DD F2 section 49.4, each with its own details. */
public final class TestcaseGenerationRejectedException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final transient Map<String, Object> details;

  private TestcaseGenerationRejectedException(String code, String message, Map<String, Object> d) {
    super(code, message);
    this.details = d;
  }

  /** PRB-041; reasons are NO_CONSTRAINTS, NOT_ENOUGH_MANUAL_SAMPLES, REFERENCE_NOT_PASSING. */
  public static TestcaseGenerationRejectedException prerequisites(List<String> reasons) {
    return new TestcaseGenerationRejectedException(
        "PRB-041", "Generation prerequisites are not met", Map.of("reasons", List.copyOf(reasons)));
  }

  /** PRB-042: no generation left for the problem. */
  public static TestcaseGenerationRejectedException quota(int used, int limit) {
    return new TestcaseGenerationRejectedException(
        "PRB-042", "No AI generation left for this problem", Map.of("used", used, "limit", limit));
  }

  /** PRB-043; reason is TIMEOUT, OUTPUT_TOO_LARGE, SCRIPT_ERROR or INVALID_FORMAT. */
  public static TestcaseGenerationRejectedException script(String reason) {
    return new TestcaseGenerationRejectedException(
        "PRB-043", "The generator script was rejected", Map.of("reason", reason));
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
