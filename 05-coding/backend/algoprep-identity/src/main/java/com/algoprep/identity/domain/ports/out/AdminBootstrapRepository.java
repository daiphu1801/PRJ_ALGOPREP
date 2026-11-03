package com.algoprep.identity.domain.ports.out;

import java.util.UUID;

/** Persistence for the first-ADMIN startup runner (DD BR-23). */
public interface AdminBootstrapRepository {

  /**
   * @return true when any user already holds a role whose base category is ADMIN
   */
  boolean adminExists();

  /**
   * Inserts the first ADMIN as {@code PENDING_VERIFICATION} with {@code must_change_password}.
   *
   * @param id new user id
   * @param email normalized email
   * @param passwordHash BCrypt hash
   * @param displayName display name
   * @return false when the email is already taken (nothing inserted)
   */
  boolean createAdmin(UUID id, String email, String passwordHash, String displayName);
}
