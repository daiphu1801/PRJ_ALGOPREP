package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.model.ReviewCacheEntry;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import com.algoprep.aireview.domain.ports.out.SolutionReviewLockPort;
import com.algoprep.aireview.domain.ports.out.SolutionReviewRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads, the per-submission lock and the two transactional writes of BR-AI-02 step 11 and BR-AI-03.
 * The LLM call runs between the reads and the writes and is never inside a transaction. Both writes
 * also insert the {@code SolutionReviewCompleted} outbox row (DD api F5 section 33) in the same
 * transaction, so the event exists exactly when the review does, cache hit included.
 */
@Service
class SolutionReviewStore {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final SolutionReviewRepository reviews;
  private final SolutionReviewLockPort locks;
  private final AiOutboxRepository outbox;
  private final IdGeneratorPort ids;
  private final ClockPort clock;

  SolutionReviewStore(
      SolutionReviewRepository reviews,
      SolutionReviewLockPort locks,
      AiOutboxRepository outbox,
      IdGeneratorPort ids,
      ClockPort clock) {
    this.reviews = reviews;
    this.locks = locks;
    this.outbox = outbox;
    this.ids = ids;
    this.clock = clock;
  }

  Optional<StoredSolutionReview> find(UUID submissionId) {
    return reviews.findBySubmission(submissionId);
  }

  <T> Optional<T> withLock(UUID submissionId, Supplier<T> work) {
    return locks.withLock(submissionId, work);
  }

  UUID newId() {
    return ids.newId();
  }

  Optional<ReviewCacheEntry> findCache(
      UUID problemId, String language, String sourceHash, UUID templateId) {
    return reviews.findCache(problemId, language, sourceHash, templateId);
  }

  /** Review plus its cache row in one transaction; a duplicate cache key keeps the review. */
  @Transactional
  boolean saveGenerated(StoredSolutionReview review) {
    if (!reviews.insertReview(review)) {
      return false;
    }
    reviews.insertCache(ids.newId(), review);
    recordCompleted(review);
    return true;
  }

  /** BR-AI-03: a new row for this submission copied from the original, then one more hit. */
  @Transactional
  Optional<StoredSolutionReview> saveFromCache(
      ReviewCacheEntry hit, SubmissionSnapshot source, String sourceHash, UUID templateId) {
    var copy =
        new StoredSolutionReview(
            ids.newId(),
            source.submissionId(),
            source.userId(),
            source.problemId(),
            source.language(),
            sourceHash,
            templateId,
            hit.resultJson(),
            hit.readabilityScore(),
            true,
            clock.now());
    if (!reviews.insertReview(copy)) {
      return Optional.empty();
    }
    reviews.incrementHit(hit.cacheId());
    recordCompleted(copy);
    return Optional.of(copy);
  }

  /** Ids and the score only: no code and no review text ever enter the event. */
  private void recordCompleted(StoredSolutionReview review) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reviewId", review.id());
    payload.put("submissionId", review.submissionId());
    payload.put("userId", review.userId());
    payload.put("problemId", review.problemId());
    payload.put("readabilityScore", review.readabilityScore());
    outbox.insert(
        new AiOutboxEvent(
            ids.newId(),
            AiOutboxEvent.SOLUTION_REVIEW_COMPLETED,
            JSON.writeValueAsString(payload),
            review.createdAt(),
            0),
        "SolutionReview",
        review.id());
  }

  Instant now() {
    return clock.now();
  }
}
