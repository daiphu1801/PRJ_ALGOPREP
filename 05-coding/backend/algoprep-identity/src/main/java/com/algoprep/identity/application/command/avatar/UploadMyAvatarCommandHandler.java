package com.algoprep.identity.application.command.avatar;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.ports.in.avatar.UploadMyAvatarUseCase;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.application.ports.out.AvatarRateLimitPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.avatar.AvatarImage;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-25: limit, then file checks (nothing touches storage yet), then the object is written BEFORE
 * the row so a failed store leaves the old avatar alone; the previous object goes after commit. Not
 * audited (the audit log is for administrative actions).
 */
@Service
class UploadMyAvatarCommandHandler implements UploadMyAvatarUseCase {
  private final AvatarRateLimitPort limiter;
  private final AvatarObjectStorePort store;
  private final UserAvatarRepository avatars;
  private final AvatarObjectCleanup cleanup;

  UploadMyAvatarCommandHandler(
      AvatarRateLimitPort limiter,
      AvatarObjectStorePort store,
      UserAvatarRepository avatars,
      AvatarObjectCleanup cleanup) {
    this.limiter = limiter;
    this.store = store;
    this.avatars = avatars;
    this.cleanup = cleanup;
  }

  @Override
  @Transactional
  public long upload(UUID userId, byte[] data) {
    if (!limiter.tryAcquire(userId)) {
      throw new RateLimitExceededException(
          IdentityErrorCodes.AVATAR_RATE_LIMITED, "Too many avatar changes in the last hour");
    }
    AvatarImage image = AvatarImageInspector.inspect(data);
    String key = "users/" + userId + "/" + UUID.randomUUID() + "." + image.extension();
    store.put(key, data, image.contentType());
    // Covers a failed commit; the catch below covers a failure inside the method.
    cleanup.deleteOnRollback(key);
    Optional<AvatarRecord> previous;
    AvatarRecord saved;
    try {
      previous = avatars.findForUpdate(userId);
      saved = avatars.upsert(userId, key, image.contentType(), data.length);
    } catch (RuntimeException failure) {
      cleanup.deleteQuietly(key);
      throw failure;
    }
    previous.ifPresent(old -> cleanup.deleteAfterCommit(old.objectKey()));
    return saved.version();
  }
}
