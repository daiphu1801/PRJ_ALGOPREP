package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.ResourceNotFoundException;

/** AIR-101: the submission does not exist (also thrown for a non-UUID id). */
public final class AiSubmissionNotFoundException extends ResourceNotFoundException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-101";

  public AiSubmissionNotFoundException() {
    super(CODE, "Submission not found");
  }
}
