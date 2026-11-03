package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.model.ProfileFields;
import java.util.Optional;
import java.util.UUID;

/** Read side of the caller's own profile. */
public interface MyProfileRepository {
  Optional<MyProfile> findById(UUID userId);

  /** Writes the five editable columns; a {@code null} optional value stores {@code NULL}. */
  void updateFields(UUID userId, ProfileFields fields);

  /** True when an account already uses this address (compared in lower case). */
  boolean emailTaken(String email);

  /**
   * Stores the new login e-mail.
   *
   * @return false when the unique index refuses it because another account got it first
   */
  boolean updateEmail(UUID userId, String email);
}
