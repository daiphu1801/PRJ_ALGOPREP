package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.RecentSolutionReview;
import java.util.List;
import java.util.UUID;

/** Endpoint 7. {@code limit} is already validated to 1..10 by the handler. */
public interface GetMyRecentSolutionReviewsUseCase {
  List<RecentSolutionReview> recent(UUID callerId, int limit);
}
