package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import java.util.UUID;

/** Endpoint 4 (DD F5 section 4). Idempotent per submission. */
public interface GetOrCreateSolutionReviewUseCase {
  StoredSolutionReview getOrCreate(UUID callerId, CallerCategory category, UUID submissionId);
}
