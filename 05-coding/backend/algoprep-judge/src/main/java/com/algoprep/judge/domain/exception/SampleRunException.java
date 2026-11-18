package com.algoprep.judge.domain.exception;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import java.util.Map;

/** Operational errors contain no source, engine diagnostics or database cause. */
public final class SampleRunException extends AlgoPrepException implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final Map<String, Object> details;

  public SampleRunException(String code) {
    this(code, Map.of());
  }

  public SampleRunException(String code, Map<String, Object> details) {
    super(code, "Sample execution unavailable");
    this.details = Map.copyOf(details);
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return details;
  }
}
