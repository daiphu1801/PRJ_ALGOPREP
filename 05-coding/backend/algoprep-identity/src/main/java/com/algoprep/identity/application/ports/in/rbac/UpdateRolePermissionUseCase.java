package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.UpdateRolePermissionCommand;
import com.algoprep.identity.domain.model.PermissionChange;

/** Switches one matrix cell on or off, effective on the next request (#31, BR-13). */
public interface UpdateRolePermissionUseCase {
  PermissionChange update(UpdateRolePermissionCommand command);
}
