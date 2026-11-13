package com.algoprep.aireview.domain.exception;

import com.algoprep.common.exception.DomainRuleViolationException;

/** AIR-102: only an ACCEPTED submission can be reviewed. */
public final class AiSubmissionNotAcceptedException extends DomainRuleViolationException {
  private static final long serialVersionUID = 1L;
  public static final String CODE = "AIR-102";

  public AiSubmissionNotAcceptedException() {
    super(CODE, "Submission is not ACCEPTED");
  }
}
