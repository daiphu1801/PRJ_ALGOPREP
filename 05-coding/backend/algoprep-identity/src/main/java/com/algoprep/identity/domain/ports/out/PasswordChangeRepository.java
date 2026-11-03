package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.PasswordChangeAccount;
import java.util.Optional;
import java.util.UUID;

/** Account operations of BR-06, all executed in the caller's transaction. */
public interface PasswordChangeRepository {

  /** Finds the account and locks its row so two concurrent changes are serialised. */
  Optional<PasswordChangeAccount> lockById(UUID userId);

  /**
   * Stores the hash, clears {@code must_change_password}, and revokes every refresh token except
   * those of {@code keepFamilyId}.
   */
  void updatePasswordAndRevokeOtherSessions(UUID userId, String passwordHash, UUID keepFamilyId);
}
