package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.CreateRoleCommand;
import com.algoprep.identity.domain.model.RoleListItem;

/** Creates a custom role without any permission (#33). */
public interface CreateRoleUseCase {
  RoleListItem create(CreateRoleCommand command);
}
