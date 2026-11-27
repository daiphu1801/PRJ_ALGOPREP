package com.algoprep.identity.domain.model;

/** Lifecycle state of an account, same value set as {@code users.status}. */
public enum UserStatus {
  ACTIVE,
  DEACTIVATED,
  PENDING_VERIFICATION
}
