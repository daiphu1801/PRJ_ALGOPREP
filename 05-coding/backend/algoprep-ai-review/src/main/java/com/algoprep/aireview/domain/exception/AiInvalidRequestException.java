package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.InvalidRequestException;

/** AIR-V01 (problemId) or AIR-V02 (limit). */
public final class AiInvalidRequestException extends InvalidRequestException {
  private static final long serialVersionUID = 1L;

  public AiInvalidRequestException(String code, String message) {
    super(code, message);
  }
}
