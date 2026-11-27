package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import java.util.Optional;
import java.util.UUID;

/** The avatar metadata row of one account (BD database section 1.16). */
public interface UserAvatarRepository {

  /** The row of {@code userId}, whatever the account status; locks it for the transaction. */
  Optional<AvatarRecord> findForUpdate(UUID userId);

  /** The row only while the account may still show it: not {@code DEACTIVATED}, not anonymised. */
  Optional<AvatarRecord> findReadable(UUID userId);

  /**
   * Inserts or replaces the row. {@code updated_at} is strictly greater than the previous one at
   * millisecond precision, so the version never repeats.
   */
  AvatarRecord upsert(UUID userId, String objectKey, String contentType, int sizeBytes);

  /** Removes the row and returns it, or empty when the user had none. */
  Optional<AvatarRecord> delete(UUID userId);
}
