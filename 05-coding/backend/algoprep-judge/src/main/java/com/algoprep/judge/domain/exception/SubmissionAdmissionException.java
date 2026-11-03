package com.algoprep.judge.domain.exception;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import java.util.Map;

/** Fixed errors for the internal admission boundary; no submitted values enter the message. */
public final class SubmissionAdmissionException extends AlgoPrepException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Map<String, Object> details;

  public SubmissionAdmissionException(String code) {
    this(code, Map.of());
  }

  public SubmissionAdmissionException(String code, Map<String, Object> details) {
    super(code, "Submission admission rejected");
    this.details = Map.copyOf(details);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
