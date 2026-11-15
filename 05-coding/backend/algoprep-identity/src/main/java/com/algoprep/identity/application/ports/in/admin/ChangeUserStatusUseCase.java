package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.application.dto.admin.ChangeUserStatusCommand;
import com.algoprep.identity.domain.model.admin.AdminUserItem;

/** Locks or unlocks an account (#37, BR-15). */
public interface ChangeUserStatusUseCase {
  AdminUserItem change(ChangeUserStatusCommand command);
}
