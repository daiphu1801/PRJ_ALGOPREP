package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.PermissionMatrix;

/** Reads the whole permission matrix (#30). */
public interface GetPermissionMatrixUseCase {
  PermissionMatrix get();
}
