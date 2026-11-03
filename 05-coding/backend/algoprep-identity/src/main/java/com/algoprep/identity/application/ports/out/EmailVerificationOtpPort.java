package com.algoprep.identity.application.ports.out;

import java.util.UUID;

/** Atomic OTP storage. Dependency errors propagate; verification never fails open. */
public interface EmailVerificationOtpPort {
  /** Stores a new digest and zero failures, TTL 600s, if the 60s resend guard is absent. */
  boolean issue(UUID userId, String digest);

  /** Checks without consuming; increments wrong attempts atomically, preserving the expiry. */
  boolean matches(UUID userId, String digest);

  /** Deletes only the matching issuance, so delayed cleanup cannot delete a newer code. */
  void clear(UUID userId, String digest);
}
