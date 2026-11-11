package com.algoprep.interviewbank.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.InvalidRequestException;
import java.util.Map;

/** INB-V07: the answer is longer than the cap. */
public final class AnswerTooLongException extends InvalidRequestException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final int maxLength;

  public AnswerTooLongException(int maxLength) {
    super("INB-V07", "answerText is longer than " + maxLength + " characters");
    this.maxLength = maxLength;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("maxLength", maxLength);
  }
}
