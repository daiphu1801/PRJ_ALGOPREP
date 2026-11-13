package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** AIR-103: another call is generating the review of this submission right now. */
public final class AiReviewInProgressException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-103";

  public AiReviewInProgressException() {
    super(CODE, "A review of this submission is being generated");
  }
}
