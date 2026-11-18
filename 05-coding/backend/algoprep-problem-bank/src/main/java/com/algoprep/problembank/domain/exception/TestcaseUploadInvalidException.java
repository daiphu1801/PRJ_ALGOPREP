package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import java.util.List;
import java.util.Map;

/** 422 PRB-045: the uploaded objects do not match what was declared. Carries no content. */
public final class TestcaseUploadInvalidException extends DomainRuleViolationException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;

  /**
   * {@code side} is input or expectedOutput; {@code problem} is MISSING, SIZE_MISMATCH,
   * INVALID_TEXT.
   */
  public record Fault(String clientKey, String side, String problem) {}

  private final Map<String, Object> details;

  public TestcaseUploadInvalidException(List<Fault> faults) {
    super("PRB-045", "Uploaded testcase objects do not match the request");
    details =
        Map.of(
            "items",
            faults.stream()
                .map(
                    f ->
                        Map.of(
                            "clientKey", f.clientKey(), "side", f.side(), "problem", f.problem()))
                .toList());
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
