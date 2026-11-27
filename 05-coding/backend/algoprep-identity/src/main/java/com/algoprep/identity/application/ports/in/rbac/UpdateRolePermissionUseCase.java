package com.algoprep.identity.application.ports.in.rbac;

import com.algoprep.identity.application.dto.rbac.UpdateRolePermissionCommand;
import com.algoprep.identity.domain.model.rbac.PermissionChange;

/** Switches one matrix cell on or off, effective on the next request (#31, BR-13). */
public interface UpdateRolePermissionUseCase {
  PermissionChange update(UpdateRolePermissionCommand command);
}
