package com.algoprep.identity.domain.ports.out;

import java.util.UUID;

/** Self-deletion and its delayed anonymisation (BR-08), each call one atomic statement set. */
public interface AccountLifecycleRepository {

  /**
   * BR-15: locks every active ADMIN account row (in id order, until the caller's transaction ends)
   * and tells whether the given user is the only one left. A user who is not an active admin is
   * never "the last admin".
   */
  boolean isLastActiveAdmin(UUID userId);

  /**
   * Deactivates an {@code ACTIVE} account as a self-deletion and revokes all its refresh tokens.
   *
   * @return false when the account was not {@code ACTIVE}, so nothing changed
   */
  boolean deactivateSelf(UUID userId);

  /**
   * Anonymises up to {@code limit} self-deleted accounts whose 30 day grace period is over, and
   * removes their linked OAuth identities. Rows locked by another transaction are skipped; accounts
   * deactivated by an admin are never selected.
   *
   * @return accounts anonymised
   */
  int anonymizeDueBatch(int limit);

  /**
   * Deletes up to {@code limit} self-registered accounts still waiting for e-mail confirmation that
   * are older than {@code ttl}, with their preferences, links and tokens; an account that already
   * has audit or activity rows is kept.
   */
  int deleteStalePendingBatch(java.time.Duration ttl, int limit);
}
