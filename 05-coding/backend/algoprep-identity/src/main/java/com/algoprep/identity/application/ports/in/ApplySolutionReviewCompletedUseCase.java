package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.CompletedSolutionReview;

/** Adds one "solution review completed" entry to the activity feed, once per event id. */
public interface ApplySolutionReviewCompletedUseCase {

  /** True when an entry was added, false when the event was a redelivery. */
  boolean apply(CompletedSolutionReview review);
}
