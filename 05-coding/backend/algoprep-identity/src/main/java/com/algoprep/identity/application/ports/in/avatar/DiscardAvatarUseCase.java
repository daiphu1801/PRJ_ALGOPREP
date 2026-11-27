package com.algoprep.identity.application.ports.in.avatar;

import java.util.UUID;

/**
 * Drops an account's avatar inside the caller's transaction, with no rate limit and no audit. Used
 * when the user deletes the account (BR-08, BR-26).
 */
public interface DiscardAvatarUseCase {
  void discard(UUID userId);
}
