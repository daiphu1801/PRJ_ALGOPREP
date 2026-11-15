package com.algoprep.identity.application.command;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.domain.exception.RbacErrorCodes;

/** One answer for an unknown role, function or action of the matrix endpoints. */
final class RbacNotFound {
  private RbacNotFound() {}

  static ResourceNotFoundException exception() {
    return new ResourceNotFoundException(
        RbacErrorCodes.RBAC_TARGET_NOT_FOUND, "Role, function or action is not available");
  }
}
