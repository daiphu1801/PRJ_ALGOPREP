package com.algoprep.identity.application.ports.in.avatar;

import java.util.UUID;

/** An administrator removes another user's avatar and the removal is audited (#77, BR-26). */
public interface RemoveUserAvatarUseCase {
  void remove(UUID actorId, UUID targetUserId);
}
