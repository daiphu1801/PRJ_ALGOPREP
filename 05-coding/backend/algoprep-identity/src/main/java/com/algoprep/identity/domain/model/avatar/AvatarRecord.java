package com.algoprep.identity.domain.model.avatar;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of {@code identity.user_avatars} (BD database section 1.16). The bytes are in object
 * storage under {@code objectKey}.
 */
public record AvatarRecord(
    UUID userId, String objectKey, String contentType, int sizeBytes, Instant updatedAt) {

  /**
   * The {@code avatarVersion} of the DTOs (DD api F1 section 0.9.5): milliseconds since the epoch.
   * The repository keeps it strictly increasing per user.
   */
  public long version() {
    return updatedAt.toEpochMilli();
  }

  /** Quoted entity tag: the {@code <avatar_id>} part of {@code users/<user>/<avatar_id>.<ext>}. */
  public String etag() {
    String name = objectKey.substring(objectKey.lastIndexOf('/') + 1);
    int dot = name.lastIndexOf('.');
    return '"' + (dot < 0 ? name : name.substring(0, dot)) + '"';
  }
}
