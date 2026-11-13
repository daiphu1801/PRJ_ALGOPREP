package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.PasswordResetChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Atomic storage for reset OTP; dependency errors never authorize a reset. */
public interface PasswordResetOtpPort {
  boolean issue(UUID userId, PasswordResetChallenge challenge);

  /** Checks without consuming. The fifth failure deletes the OTP, not the cooldown. */
  Optional<Instant> verify(UUID userId, String digest);

  /** Throttles OAuth guidance without creating an OTP. */
  boolean reserveGuidance(UUID userId, Instant issuedAt);

  /** Removes only this issuance so delayed cleanup cannot remove a newer challenge. */
  void clear(UUID userId, Instant issuedAt);
}
