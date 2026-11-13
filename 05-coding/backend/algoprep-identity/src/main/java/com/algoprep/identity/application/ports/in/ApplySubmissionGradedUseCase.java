package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.GradedSubmission;

/** Applies one {@code SubmissionGraded} event to the F1 read models, once per event id. */
public interface ApplySubmissionGradedUseCase {

  /** True when the event changed the read models, false when it was a redelivery. */
  boolean apply(GradedSubmission submission);
}
