package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token row as the use cases see it.
 *
 * @param id row id
 * @param userId owner
 * @param familyId rotation chain; every token issued from one login shares it
 * @param expiresAt natural expiry
 * @param revokedAt when it was revoked, {@code null} while usable
 */
public record StoredRefreshToken(
    UUID id, UUID userId, UUID familyId, Instant expiresAt, Instant revokedAt) {

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpiredAt(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
