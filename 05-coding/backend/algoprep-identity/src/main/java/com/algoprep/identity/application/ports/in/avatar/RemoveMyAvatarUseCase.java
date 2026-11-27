package com.algoprep.identity.application.ports.in.avatar;

import java.util.UUID;

/** Removes the caller's own avatar; idempotent (#75, BR-26). */
public interface RemoveMyAvatarUseCase {
  void remove(UUID userId);
}
