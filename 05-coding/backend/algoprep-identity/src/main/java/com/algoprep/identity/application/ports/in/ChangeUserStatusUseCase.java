package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.ChangeUserStatusCommand;
import com.algoprep.identity.domain.model.AdminUserItem;

/** Locks or unlocks an account (#37, BR-15). */
public interface ChangeUserStatusUseCase {
  AdminUserItem change(ChangeUserStatusCommand command);
}
