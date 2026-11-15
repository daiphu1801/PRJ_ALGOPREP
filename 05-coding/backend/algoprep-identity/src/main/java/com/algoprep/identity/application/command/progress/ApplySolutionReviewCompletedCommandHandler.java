package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.in.ApplySolutionReviewCompletedUseCase;
import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.CompletedSolutionReview;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD api F1 section 2, {@code SolutionReviewCompleted}: one {@code SOLUTION_REVIEW_COMPLETED} entry
 * in the personal feed of the learner (no class: the DD gives the entry no class scope). The
 * processed event row and the feed row commit together, so a redelivery adds nothing.
 */
@Service
class ApplySolutionReviewCompletedCommandHandler implements ApplySolutionReviewCompletedUseCase {
  static final String EVENT_TYPE = "SolutionReviewCompleted";
  static final String ACTIVITY_TYPE = "SOLUTION_REVIEW_COMPLETED";
  static final String REF_TYPE = "SOLUTION_REVIEW";

  private final ProcessedEventRepository processed;
  private final RecentActivityRepository feed;
  private final IdGeneratorPort ids;

  ApplySolutionReviewCompletedCommandHandler(
      ProcessedEventRepository processed, RecentActivityRepository feed, IdGeneratorPort ids) {
    this.processed = processed;
    this.feed = feed;
    this.ids = ids;
  }

  @Override
  @Transactional
  public boolean apply(CompletedSolutionReview r) {
    if (!processed.markProcessed(r.eventId(), EVENT_TYPE)) {
      return false;
    }
    feed.add(
        new ActivityEntry(
            ids.newId(), r.userId(), null, ACTIVITY_TYPE, REF_TYPE, r.reviewId(), r.occurredAt()));
    return true;
  }
}
