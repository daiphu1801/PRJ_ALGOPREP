package com.algoprep.identity.application.ports.in;

import java.util.UUID;

/** Deletes an unused custom role together with its cells (#34). */
public interface DeleteRoleUseCase {
  void delete(UUID actorId, UUID roleId);
}
