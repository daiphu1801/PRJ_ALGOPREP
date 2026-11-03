package com.algoprep.identity.domain.model;

import java.time.Instant;

/** Account snapshot read under a user row lock, with timestamps from the PostgreSQL clock. */
public record PasswordResetAccount(
    UserAccount user, String email, Instant passwordChangedAt, Instant databaseTime) {
  /** Self-registration must complete email verification before this recovery flow. */
  public boolean canResetPassword() {
    return user.passwordHash() != null
        && !(user.status() == UserStatus.PENDING_VERIFICATION && !user.mustChangePassword());
  }

  @Override
  public String toString() {
    return "PasswordResetAccount[redacted]";
  }
}
