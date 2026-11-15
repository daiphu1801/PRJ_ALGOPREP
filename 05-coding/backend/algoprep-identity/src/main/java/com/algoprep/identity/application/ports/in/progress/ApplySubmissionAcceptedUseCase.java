package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.AcceptedSubmission;

/** Adds one "submission accepted" entry to the activity feed, once per event id. */
public interface ApplySubmissionAcceptedUseCase {

  /** True when an entry was added, false when the event was a redelivery. */
  boolean apply(AcceptedSubmission submission);
}
