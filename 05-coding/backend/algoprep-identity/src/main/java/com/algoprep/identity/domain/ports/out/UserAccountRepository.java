package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.UserAccount;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Reads and updates the account fields that authentication touches. */
public interface UserAccountRepository {

  /**
   * @param email address to look up, compared case-insensitively
   * @return the account, if any
   */
  Optional<UserAccount> findByEmail(String email);

  /**
   * @param id user id
   * @return the account, if any
   */
  Optional<UserAccount> findById(UUID id);

  /**
   * Moves an Admin-created account from {@code PENDING_VERIFICATION} to {@code ACTIVE} (DD BR-21).
   *
   * @param id user id
   */
  void markActive(UUID id);

  /**
   * Records the latest activity, at most once a minute per user (BD 1.1 {@code last_active_at}).
   *
   * @param id user id
   * @param now current time
   */
  void touchLastActive(UUID id, Instant now);
}
