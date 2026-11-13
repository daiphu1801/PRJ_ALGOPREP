package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.RecentSolutionReview;
import java.util.Optional;
import java.util.UUID;

/** Endpoint 6: the newest review of the caller for one problem. */
public interface GetMySolutionReviewQuickInsightUseCase {
  Optional<RecentSolutionReview> get(UUID callerId, UUID problemId);
}
