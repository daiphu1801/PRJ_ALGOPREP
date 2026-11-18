package com.algoprep.identity.application.ports.in.rbac;

import com.algoprep.identity.domain.model.rbac.PermissionMatrix;

/** Reads the whole permission matrix (#30). */
public interface GetPermissionMatrixUseCase {
  PermissionMatrix get();
}
