package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.application.dto.admin.CreateUserAccountCommand;
import com.algoprep.identity.domain.model.admin.AdminUserItem;

/** Admin creates a STUDENT or INSTRUCTOR account with a temporary password (#50, BR-21). */
public interface CreateUserAccountUseCase {
  AdminUserItem create(CreateUserAccountCommand command);
}
