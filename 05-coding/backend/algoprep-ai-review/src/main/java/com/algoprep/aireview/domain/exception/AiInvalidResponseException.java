package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.AlgoPrepException;

/** AIR-502: the LLM answered with JSON that does not satisfy the schema (maps to HTTP 502). */
public final class AiInvalidResponseException extends AlgoPrepException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-502";

  public AiInvalidResponseException(String message) {
    super(CODE, message);
  }

  public AiInvalidResponseException(String message, Throwable cause) {
    super(CODE, message, cause);
  }
}
