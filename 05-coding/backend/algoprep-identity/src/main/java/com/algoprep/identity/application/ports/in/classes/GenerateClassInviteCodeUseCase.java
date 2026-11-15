package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassInviteCode;
import java.util.UUID;

/** Issues one more invite code for an owned class (#23). */
public interface GenerateClassInviteCodeUseCase {
  ClassInviteCode generate(UUID instructorId, UUID classId);
}
