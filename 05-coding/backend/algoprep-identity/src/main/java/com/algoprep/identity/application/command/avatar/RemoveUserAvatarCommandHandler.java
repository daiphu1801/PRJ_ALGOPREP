package com.algoprep.identity.application.command.avatar;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.command.RbacAuditJson;
import com.algoprep.identity.application.ports.in.avatar.RemoveUserAvatarUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.audit.AuditEntry;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-26, #77: the target must exist (404 IDT-450). A removal that deleted a row writes exactly one
 * {@code USER_AVATAR_REMOVED} audit row in the same transaction, so a failing audit rolls the
 * removal back; no row means no audit and no storage call. Not counted by the hourly limit.
 */
@Service
class RemoveUserAvatarCommandHandler implements RemoveUserAvatarUseCase {
  private final AdminUserRepository users;
  private final UserAvatarRepository avatars;
  private final AuditLogPort audit;
  private final AvatarObjectCleanup cleanup;

  RemoveUserAvatarCommandHandler(
      AdminUserRepository users,
      UserAvatarRepository avatars,
      AuditLogPort audit,
      AvatarObjectCleanup cleanup) {
    this.users = users;
    this.avatars = avatars;
    this.audit = audit;
    this.cleanup = cleanup;
  }

  @Override
  @Transactional
  public void remove(UUID actorId, UUID targetUserId) {
    users
        .lock(targetUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    IdentityErrorCodes.USER_NOT_FOUND, "Account is not available"));
    Optional<AvatarRecord> removed = avatars.delete(targetUserId);
    if (removed.isEmpty()) {
      return;
    }
    audit.record(
        new AuditEntry(
            actorId,
            "USER_AVATAR_REMOVED",
            "USER",
            targetUserId.toString(),
            RbacAuditJson.of("hadAvatar", true),
            null));
    cleanup.deleteAfterCommit(removed.get().objectKey());
  }
}
