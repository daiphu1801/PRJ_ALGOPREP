package com.algoprep.identity.domain.model;

import java.time.Instant;

/** Only the digest and DB issuance timestamp cross the OTP storage boundary. */
public record PasswordResetChallenge(String digest, Instant issuedAt) {
  @Override
  public String toString() {
    return "PasswordResetChallenge[redacted]";
  }
}
