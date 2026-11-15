package com.algoprep.identity.application.ports.in;

import java.util.UUID;

/** Sends a temporary password to a user, or sign-in guidance when there is no password (#38). */
public interface ResetUserPasswordUseCase {
  void reset(UUID actorId, UUID userId);
}
