package com.algoprep.identity.application.ports.in.classes;

import java.util.UUID;

/** Removes one invite code of an owned class so it can no longer be used (#78). */
public interface RevokeClassInviteCodeUseCase {
  /** Calling it again for a code that is already gone is not an error. */
  void revoke(UUID instructorId, UUID classId, UUID codeId);
}
