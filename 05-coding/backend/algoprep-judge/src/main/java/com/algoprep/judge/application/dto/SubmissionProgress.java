package com.algoprep.judge.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Closed, content-free realtime schema from F4 API sections 2.3 and 2.4. */
public sealed interface SubmissionProgress
    permits SubmissionStatusProgress, TestcaseProgress, SubmissionFinishedProgress {
  UUID submissionId();

  Instant occurredAt();

  @JsonProperty
  String type();

  @JsonProperty
  int seq();

  @JsonProperty("isFinal")
  boolean isFinal();
}
