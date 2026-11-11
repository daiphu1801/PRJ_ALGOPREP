package com.algoprep.aireview.domain.model;

import java.util.Objects;
import java.util.UUID;

/** One ledger row of {@code token_usage}; {@code callId} is the idempotency key. */
public record TokenUsageRecord(
    UUID callId,
    UUID userId,
    FeatureCode featureCode,
    UUID problemId,
    UUID ownerId,
    int tokensPrompt,
    int tokensCompletion,
    String modelName) {
  public TokenUsageRecord {
    Objects.requireNonNull(callId, "callId");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(featureCode, "featureCode");
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(modelName, "modelName");
  }
}
