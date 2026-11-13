package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.PasswordResetAccount;
import java.util.Optional;
import java.util.UUID;

/** Account operations of BR-04, all executed in the caller's transaction. */
public interface PasswordResetRepository {
  Optional<PasswordResetAccount> lockByEmail(String email);

  /** Updates password/DB timestamp/flags and revokes all current refresh tokens. */
  void updatePasswordAndRevokeSessions(UUID userId, String passwordHash);
}
