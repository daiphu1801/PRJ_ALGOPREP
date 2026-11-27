package com.algoprep.identity.application.ports.in.progress;

import com.algoprep.identity.domain.model.progress.FinishedMockInterview;

/** Adds one "mock interview finished" entry to the activity feed, once per event id. */
public interface ApplyMockInterviewFinishedUseCase {

  /** True when an entry was added, false when the event was a redelivery. */
  boolean apply(FinishedMockInterview interview);
}
