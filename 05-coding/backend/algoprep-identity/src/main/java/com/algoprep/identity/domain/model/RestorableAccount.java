package com.algoprep.identity.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Lifecycle fields read under a user row lock; the time is from the database after locking. */
public record RestorableAccount(
    UUID id,
    UserStatus status,
    String deactivationSource,
    Instant deactivatedAt,
    Instant anonymizedAt,
    Instant databaseTime) {
  public boolean canRestore() {
    return status == UserStatus.DEACTIVATED
        && "SELF".equals(deactivationSource)
        && anonymizedAt == null
        && deactivatedAt != null
        && !deactivatedAt.isAfter(databaseTime)
        && deactivatedAt.plus(Duration.ofDays(30)).isAfter(databaseTime);
  }
}
