package com.algoprep.identity.application.command.avatar;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.ports.in.avatar.RemoveMyAvatarUseCase;
import com.algoprep.identity.application.ports.out.AvatarRateLimitPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-26, #75: shares the hourly limit with #74; no avatar is a quiet no-op. */
@Service
class RemoveMyAvatarCommandHandler implements RemoveMyAvatarUseCase {
  private final AvatarRateLimitPort limiter;
  private final UserAvatarRepository avatars;
  private final AvatarObjectCleanup cleanup;

  RemoveMyAvatarCommandHandler(
      AvatarRateLimitPort limiter, UserAvatarRepository avatars, AvatarObjectCleanup cleanup) {
    this.limiter = limiter;
    this.avatars = avatars;
    this.cleanup = cleanup;
  }

  @Override
  @Transactional
  public void remove(UUID userId) {
    if (!limiter.tryAcquire(userId)) {
      throw new RateLimitExceededException(
          IdentityErrorCodes.AVATAR_RATE_LIMITED, "Too many avatar changes in the last hour");
    }
    avatars.delete(userId).ifPresent(row -> cleanup.deleteAfterCommit(row.objectKey()));
  }
}
