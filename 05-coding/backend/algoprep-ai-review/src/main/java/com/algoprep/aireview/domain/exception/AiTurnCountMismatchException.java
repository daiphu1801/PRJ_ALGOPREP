package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.ResourceConflictException;
import java.util.Map;

/** AIR-112: the client's {@code expectedTurnCount} differs from the stored {@code turn_count}. */
public final class AiTurnCountMismatchException extends ResourceConflictException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-112";

  private final int turnCount;

  public AiTurnCountMismatchException(int turnCount) {
    super(CODE, "expectedTurnCount does not match the session turn count");
    this.turnCount = turnCount;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return Map.of("turnCount", turnCount);
  }
}
