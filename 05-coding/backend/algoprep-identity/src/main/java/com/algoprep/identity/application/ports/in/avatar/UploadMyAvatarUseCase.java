package com.algoprep.identity.application.ports.in.avatar;

import java.util.UUID;

/** Adds or replaces the caller's avatar (#74, BR-25). */
public interface UploadMyAvatarUseCase {

  /** Validates, stores and returns the new {@code avatarVersion}. */
  long upload(UUID userId, byte[] data);
}
