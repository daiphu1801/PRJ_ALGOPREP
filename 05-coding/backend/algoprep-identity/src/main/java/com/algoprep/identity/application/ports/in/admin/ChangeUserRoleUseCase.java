package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.application.dto.admin.ChangeUserRoleCommand;
import com.algoprep.identity.domain.model.admin.AdminUserItem;

/** Moves an account to another role (#36, BR-15). */
public interface ChangeUserRoleUseCase {
  AdminUserItem change(ChangeUserRoleCommand command);
}
