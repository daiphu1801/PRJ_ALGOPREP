package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.MySettings;
import java.util.Optional;
import java.util.UUID;

/** Settings of one account across {@code user_preferences} and {@code users}. */
public interface MySettingsRepository {

  /** The stored settings, with defaults for a missing preferences row. */
  Optional<MySettings> find(UUID userId);

  /**
   * Same as {@link #find} but locks the account row until the caller's transaction ends, so two
   * concurrent read-merge-save updates run one after the other and neither loses its values.
   */
  Optional<MySettings> findForUpdate(UUID userId);

  /** Writes all six values in the caller's transaction, creating the preferences row if needed. */
  void save(UUID userId, MySettings settings);
}
