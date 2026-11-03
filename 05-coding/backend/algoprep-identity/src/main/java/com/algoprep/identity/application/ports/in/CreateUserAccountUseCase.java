package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.CreateUserAccountCommand;
import com.algoprep.identity.domain.model.AdminUserItem;

/** Admin creates a STUDENT or INSTRUCTOR account with a temporary password (#50, BR-21). */
public interface CreateUserAccountUseCase {
  AdminUserItem create(CreateUserAccountCommand command);
}
