package com.algoprep.identity.application.command.avatar;

import com.algoprep.identity.application.ports.in.avatar.DiscardAvatarUseCase;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-08 and BR-26: runs inside the account-deletion transaction (REQUIRED), so the row goes with
 * the status change; the object follows after commit. No limit, no audit.
 */
@Service
class DiscardAvatarCommandHandler implements DiscardAvatarUseCase {
  private final UserAvatarRepository avatars;
  private final AvatarObjectCleanup cleanup;

  DiscardAvatarCommandHandler(UserAvatarRepository avatars, AvatarObjectCleanup cleanup) {
    this.avatars = avatars;
    this.cleanup = cleanup;
  }

  @Override
  @Transactional
  public void discard(UUID userId) {
    avatars.delete(userId).ifPresent(row -> cleanup.deleteAfterCommit(row.objectKey()));
  }
}
