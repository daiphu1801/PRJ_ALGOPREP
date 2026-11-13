package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetPermissionMatrixUseCase;
import com.algoprep.identity.domain.model.PermissionMatrix;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetPermissionMatrixQueryHandler implements GetPermissionMatrixUseCase {
  private final RbacRepository rbac;

  GetPermissionMatrixQueryHandler(RbacRepository rbac) {
    this.rbac = rbac;
  }

  @Override
  @Transactional(readOnly = true)
  public PermissionMatrix get() {
    return new PermissionMatrix(rbac.roles(), rbac.functions(), rbac.actions(), rbac.cells());
  }
}
