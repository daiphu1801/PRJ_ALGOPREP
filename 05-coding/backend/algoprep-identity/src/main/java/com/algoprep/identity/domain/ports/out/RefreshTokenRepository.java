package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.StoredRefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persistence of refresh tokens; only hashes are stored (BD security section 1). */
public interface RefreshTokenRepository {

  /**
   * @param token row to store
   * @param tokenHash SHA-256 hex of the raw token
   * @param issuedAt issue time
   */
  void insert(StoredRefreshToken token, String tokenHash, Instant issuedAt);

  /**
   * Finds a token and locks its row so two concurrent refreshes of the same token are serialised.
   *
   * @param tokenHash SHA-256 hex of the raw token
   * @return the token, if any
   */
  Optional<StoredRefreshToken> findByHashForUpdate(String tokenHash);

  /**
   * @param id row id
   * @param at revocation time
   */
  void revoke(UUID id, Instant at);

  /**
   * Revokes every still-usable token of a family.
   *
   * @param familyId rotation chain
   * @param at revocation time
   */
  void revokeFamily(UUID familyId, Instant at);

  /**
   * Deletes up to {@code limit} tokens that are expired or revoked AND issued more than six months
   * ago by the database clock (JOB-ID-04; BD 1.6 keeps issued_at for the ADM0101 retention chart).
   * Still-usable tokens are never deleted.
   *
   * @param limit batch size
   * @return rows deleted
   */
  int deleteExpiredBatch(int limit);
}
