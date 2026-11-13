package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.ChangeUserRoleCommand;
import com.algoprep.identity.domain.model.AdminUserItem;

/** Moves an account to another role (#36, BR-15). */
public interface ChangeUserRoleUseCase {
  AdminUserItem change(ChangeUserRoleCommand command);
}
