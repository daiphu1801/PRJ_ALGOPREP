package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarded finalization and Outbox writes share one transaction; never publish domain events here.
 */
public interface QueueFailureRepository {
  Optional<SubmissionLifecycleView> finishPending(UUID submissionId);

  Optional<SubmissionLifecycleView> find(UUID submissionId);

  boolean firstAttempt(SubmissionLifecycleView submission);

  void appendGraded(UUID eventId, UUID submissionId, String envelope, Instant finishedAt);
}
