package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.model.ReviewCacheEntry;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tables {@code solution_reviews} and {@code ai_cache_entries}. */
public interface SolutionReviewRepository {
  Optional<StoredSolutionReview> findBySubmission(UUID submissionId);

  Optional<ReviewCacheEntry> findCache(
      UUID problemId, String language, String sourceHash, UUID promptTemplateId);

  /** False when the unique {@code submission_id} was already taken (nothing inserted). */
  boolean insertReview(StoredSolutionReview review);

  /** False when the cache key already existed (the review is kept, the cache row is skipped). */
  boolean insertCache(UUID cacheId, StoredSolutionReview original);

  void incrementHit(UUID cacheId);

  List<UUID> distinctProblemIds(UUID userId);

  Optional<RecentSolutionReview> latestForProblem(UUID userId, UUID problemId);

  List<RecentSolutionReview> recent(UUID userId, int limit);
}
