package com.algoprep.identity.application.ports.in.rbac;

import com.algoprep.identity.application.dto.rbac.CreateRoleCommand;
import com.algoprep.identity.domain.model.rbac.RoleListItem;

/** Creates a custom role without any permission (#33). */
public interface CreateRoleUseCase {
  RoleListItem create(CreateRoleCommand command);
}
