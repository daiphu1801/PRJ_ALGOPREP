package com.algoprep.identity.application.command.avatar;

import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.model.avatar.AvatarImage;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The store-then-row part of BR-25 for an imported picture: no rate limit, no audit, and never
 * replaces an avatar the user already has. Separate from the importer so the network download runs
 * before any database connection is taken.
 */
@Service
class OAuthAvatarSaver {
  private final AvatarObjectStorePort store;
  private final UserAvatarRepository avatars;
  private final AvatarObjectCleanup cleanup;

  OAuthAvatarSaver(
      AvatarObjectStorePort store, UserAvatarRepository avatars, AvatarObjectCleanup cleanup) {
    this.store = store;
    this.avatars = avatars;
    this.cleanup = cleanup;
  }

  /** Returns false when the user already has an avatar and nothing was stored. */
  @Transactional
  boolean saveIfAbsent(UUID userId, byte[] data, AvatarImage image) {
    if (avatars.findForUpdate(userId).isPresent()) {
      return false;
    }
    String key = "users/" + userId + "/" + UUID.randomUUID() + "." + image.extension();
    store.put(key, data, image.contentType());
    cleanup.deleteOnRollback(key);
    try {
      avatars.upsert(userId, key, image.contentType(), data.length);
    } catch (RuntimeException failure) {
      cleanup.deleteQuietly(key);
      throw failure;
    }
    return true;
  }
}
