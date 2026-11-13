package com.algoprep.judge.domain.model;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import java.time.Instant;
import java.util.UUID;

/** Minimal trusted state for queue-failure resolution; never includes submitted source. */
public record SubmissionLifecycleView(
    UUID submissionId, Owner owner, String language, String status, Timing timing) {
  public record Timing(Instant submittedAt, Instant finishedAt) {}
}
